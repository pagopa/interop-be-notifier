package it.pagopa.interop.notifier.server.impl

import akka.actor.typed.{ActorSystem, Behavior}
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityContext}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.SecurityDirectives
import akka.persistence.typed.PersistenceId
import com.atlassian.oai.validator.report.ValidationReport
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import it.pagopa.interop.agreementmanagement.model.persistence.AgreementEventsSerde.jsonToAgreement
import it.pagopa.interop.commons.jwt._
import it.pagopa.interop.commons.jwt.service.JWTReader
import it.pagopa.interop.commons.jwt.service.impl.{DefaultInteropTokenGenerator, DefaultJWTReader, getClaimsVerifier}
import it.pagopa.interop.commons.queue.QueueReader
import it.pagopa.interop.commons.signer.service.impl.KMSSignerServiceImpl
import it.pagopa.interop.commons.utils.AkkaUtils.PassThroughAuthenticator
import it.pagopa.interop.commons.utils.OpenapiUtils
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.commons.utils.errors.GenericComponentErrors.ValidationRequestError
import it.pagopa.interop.notifier.api.impl.{
  EventsApiMarshallerImpl,
  EventsServiceApiImpl,
  HealthApiMarshallerImpl,
  HealthServiceApiImpl,
  problemOf
}
import it.pagopa.interop.notifier.api.{EventsApi, HealthApi}
import it.pagopa.interop.notifier.common.system.ApplicationConfiguration
import it.pagopa.interop.notifier.model.persistence.{Command, OrganizationNotificationEventIdBehavior}
import it.pagopa.interop.notifier.service._
import it.pagopa.interop.notifier.service.impl._
import it.pagopa.interop.purposemanagement.model.persistence.PurposeEventsSerde.jsonToPurpose

import scala.concurrent.{ExecutionContext, Future}

trait Dependencies {

  def sqsReader()(implicit ec: ExecutionContext): QueueReader = QueueReader.get(ApplicationConfiguration.queueURL) {
    jsonToPurpose orElse jsonToAgreement
  }

  def getJwtReader: Future[JWTReader] = JWTConfiguration.jwtReader
    .loadKeyset()
    .map(keyset =>
      new DefaultJWTReader with PublicKeysHolder {
        var publicKeyset: Map[KID, SerializedKey]                                        = keyset
        override protected val claimsVerifier: DefaultJWTClaimsVerifier[SecurityContext] =
          getClaimsVerifier(audience = ApplicationConfiguration.interopAudience)
      }
    )
    .toFuture

  private val notificationBehaviorFactory: EntityContext[Command] => Behavior[Command] = { entityContext =>
    OrganizationNotificationEventIdBehavior(
      entityContext.shard,
      PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId)
    )
  }

  val organizationNotificationEntity: Entity[Command, ShardingEnvelope[Command]] =
    Entity(OrganizationNotificationEventIdBehavior.TypeKey)(notificationBehaviorFactory)

  def dynamoReader()(implicit ec: ExecutionContext): DynamoServiceImpl =
    new DynamoServiceImpl(ApplicationConfiguration.dynamoTableName)

  def eventsApi(dynamoReader: DynamoService, jwtReader: JWTReader)(implicit ec: ExecutionContext): EventsApi =
    new EventsApi(
      new EventsServiceApiImpl(dynamoReader),
      EventsApiMarshallerImpl,
      jwtReader.OAuth2JWTValidatorAsContexts
    )

  val validationExceptionToRoute: ValidationReport => Route = report => {
    val error =
      problemOf(StatusCodes.BadRequest, ValidationRequestError(OpenapiUtils.errorFromRequestValidationReport(report)))
    complete(error.status, error)(HealthApiMarshallerImpl.toEntityMarshallerProblem)
  }

  val healthApi: HealthApi = new HealthApi(
    new HealthServiceApiImpl(),
    HealthApiMarshallerImpl,
    SecurityDirectives.authenticateOAuth2("SecurityRealm", PassThroughAuthenticator)
  )

  def catalogManagementService()(implicit ec: ExecutionContext, actorSystem: ActorSystem[_]): CatalogManagementService =
    CatalogManagementServiceImpl(
      CatalogManagementInvoker()(actorSystem.classicSystem),
      CatalogManagementApi(ApplicationConfiguration.catalogManagementURL)
    )

  def agreementManagementService()(implicit
    ec: ExecutionContext,
    actorSystem: ActorSystem[_]
  ): AgreementManagementService =
    AgreementManagementServiceImpl(
      AgreementManagementInvoker()(actorSystem.classicSystem),
      AgreementManagementApi(ApplicationConfiguration.agreementManagementURL)
    )

  def purposeManagementService()(implicit ec: ExecutionContext, actorSystem: ActorSystem[_]): PurposeManagementService =
    PurposeManagementServiceImpl(
      PurposeManagementInvoker()(actorSystem.classicSystem),
      PurposeManagementApi(ApplicationConfiguration.purposeManagementURL)
    )

  def eventIdRetriever(sharding: ClusterSharding)(implicit ec: ExecutionContext, actorSystem: ActorSystem[_]) =
    new EventIdRetriever(actorSystem, sharding, entity = organizationNotificationEntity)

  def interopTokenGenerator(implicit ec: ExecutionContext, actorSystem: ActorSystem[_]) =
    new DefaultInteropTokenGenerator(
      KMSSignerServiceImpl()(actorSystem.classicSystem),
      new PrivateKeysKidHolder {
        override val RSAPrivateKeyset: Set[KID] = ApplicationConfiguration.rsaKeysIdentifiers
        override val ECPrivateKeyset: Set[KID]  = ApplicationConfiguration.ecKeysIdentifiers
      }
    )
}
