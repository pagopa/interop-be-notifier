package it.pagopa.interop.notifier.server.impl

import akka.actor.typed.{ActorSystem, Behavior}
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityContext, ShardedDaemonProcess}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.SecurityDirectives
import akka.persistence.typed.PersistenceId
import akka.projection.ProjectionBehavior
import com.atlassian.oai.validator.report.ValidationReport
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.agreementmanagement.model.persistence.AgreementEventsSerde.jsonToAgreement
import it.pagopa.interop.authorizationmanagement.model.persistence.AuthorizationEventsSerde.jsonToAuth
import it.pagopa.interop.catalogmanagement.model.persistence.CatalogEventsSerde.jsonToCatalog
import it.pagopa.interop.commons.jwt._
import it.pagopa.interop.commons.jwt.service.JWTReader
import it.pagopa.interop.commons.jwt.service.impl.{DefaultInteropTokenGenerator, DefaultJWTReader, getClaimsVerifier}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.queue.QueueReader
import it.pagopa.interop.commons.signer.service.impl.KMSSignerService
import it.pagopa.interop.commons.utils.AkkaUtils.PassThroughAuthenticator
import it.pagopa.interop.commons.utils.OpenapiUtils
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.commons.utils.errors.{ServiceCode, Problem => CommonProblem}
import it.pagopa.interop.notifier.api.impl.{
  EventsApiMarshallerImpl,
  EventsServiceApiImpl,
  HealthApiMarshallerImpl,
  HealthServiceApiImpl
}
import it.pagopa.interop.notifier.api.{EventsApi, HealthApi}
import it.pagopa.interop.notifier.common.system.ApplicationConfiguration
import it.pagopa.interop.notifier.common.system.ApplicationConfiguration.{numberOfProjectionTags, projectionTag}
import it.pagopa.interop.notifier.model.persistence.projection.NotifierCqrsProjection
import it.pagopa.interop.notifier.model.persistence.{Command, OrganizationNotificationEventIdBehavior}
import it.pagopa.interop.notifier.service._
import it.pagopa.interop.notifier.service.impl._
import it.pagopa.interop.purposemanagement.model.persistence.PurposeEventsSerde.jsonToPurpose
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

trait Dependencies {

  implicit val serviceCode: ServiceCode = ServiceCode("017")

  implicit val loggerTI: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog]("OAuth2JWTValidatorAsContexts")

  def sqsReader()(implicit ec: ExecutionContext): QueueReader = QueueReader.get(ApplicationConfiguration.queueURL) {
    jsonToPurpose orElse jsonToAgreement orElse jsonToCatalog orElse jsonToAuth
  }

  def getJwtReader(): Future[JWTReader] = JWTConfiguration.jwtReader
    .loadKeyset()
    .map(keyset =>
      new DefaultJWTReader with PublicKeysHolder {
        var publicKeyset: Map[KID, SerializedKey]                                        = keyset
        override protected val claimsVerifier: DefaultJWTClaimsVerifier[SecurityContext] =
          getClaimsVerifier(audience = ApplicationConfiguration.interopAudience)
      }
    )
    .toFuture

  def initProjections()(implicit actorSystem: ActorSystem[_], ec: ExecutionContext): Unit = {
    val dbConfig: DatabaseConfig[JdbcProfile] =
      DatabaseConfig.forConfig("akka-persistence-jdbc.shared-databases.slick")

    val mongoDbConfig = ApplicationConfiguration.mongoDb

    val projectionId   = "notifier-cqrs-projections"
    val cqrsProjection = NotifierCqrsProjection.projection(dbConfig, mongoDbConfig, projectionId)

    ShardedDaemonProcess(actorSystem).init[ProjectionBehavior.Command](
      name = projectionId,
      numberOfInstances = numberOfProjectionTags,
      behaviorFactory = (i: Int) => ProjectionBehavior(cqrsProjection.projection(projectionTag(i))),
      stopMessage = ProjectionBehavior.Stop
    )
  }

  val notificationBehaviorFactory: EntityContext[Command] => Behavior[Command] = { entityContext =>
    val i = math.abs(entityContext.entityId.hashCode % numberOfProjectionTags)
    OrganizationNotificationEventIdBehavior(
      entityContext.shard,
      PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId),
      projectionTag(i)
    )
  }

  val organizationNotificationEntity: Entity[Command, ShardingEnvelope[Command]] =
    Entity(OrganizationNotificationEventIdBehavior.TypeKey)(notificationBehaviorFactory)

  def eventsApi(dynamoNotificationService: DynamoNotificationService, jwtReader: JWTReader)(implicit
    ec: ExecutionContext
  ): EventsApi = new EventsApi(
    new EventsServiceApiImpl(dynamoNotificationService),
    EventsApiMarshallerImpl,
    jwtReader.OAuth2JWTValidatorAsContexts
  )

  val validationExceptionToRoute: ValidationReport => Route = report => {
    val error =
      CommonProblem(StatusCodes.BadRequest, OpenapiUtils.errorFromRequestValidationReport(report), serviceCode, None)
    complete(error.status, error)
  }

  val healthApi: HealthApi = new HealthApi(
    HealthServiceApiImpl,
    HealthApiMarshallerImpl,
    SecurityDirectives.authenticateOAuth2("SecurityRealm", PassThroughAuthenticator),
    loggingEnabled = false
  )

  def catalogManagementService(
    blockingEc: ExecutionContextExecutor
  )(implicit ec: ExecutionContext, actorSystem: ActorSystem[_]): CatalogManagementService =
    new CatalogManagementServiceImpl(
      CatalogManagementInvoker(blockingEc)(actorSystem.classicSystem),
      CatalogManagementApi(ApplicationConfiguration.catalogManagementURL)
    )

  def authorizationEventsHandler(blockingEc: ExecutionContextExecutor): AuthorizationEventsHandler =
    new AuthorizationEventsHandler(blockingEc)

  def eventIdRetriever(
    sharding: ClusterSharding
  )(implicit ec: ExecutionContext, actorSystem: ActorSystem[_]): EventIdRetriever =
    new EventIdRetriever(actorSystem, sharding, entity = organizationNotificationEntity)

  def interopTokenGenerator(blockingEc: ExecutionContextExecutor)(implicit ex: ExecutionContext) =
    new DefaultInteropTokenGenerator(
      new KMSSignerService(blockingEc),
      new PrivateKeysKidHolder {
        override val RSAPrivateKeyset: Set[KID] = ApplicationConfiguration.rsaKeysIdentifiers
        override val ECPrivateKeyset: Set[KID]  = ApplicationConfiguration.ecKeysIdentifiers
      }
    )
}
