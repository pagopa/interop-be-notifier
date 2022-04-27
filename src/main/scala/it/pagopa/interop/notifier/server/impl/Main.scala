package it.pagopa.interop.notifier.server.impl

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import akka.cluster.ClusterEvent
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityContext}
import akka.cluster.typed.{Cluster, Subscribe}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.directives.SecurityDirectives
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.persistence.typed.PersistenceId
import akka.{actor => classic}
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import it.pagopa.interop.agreementmanagement.model.persistence.AgreementEventsSerde.jsonToAgreement
import it.pagopa.interop.commons.jwt._
import it.pagopa.interop.commons.jwt.service.impl.{DefaultInteropTokenGenerator, DefaultJWTReader, getClaimsVerifier}
import it.pagopa.interop.commons.jwt.service.{InteropTokenGenerator, JWTReader}
import it.pagopa.interop.commons.queue.{QueueConfiguration, QueueReader}
import it.pagopa.interop.commons.utils.AkkaUtils.PassThroughAuthenticator
import it.pagopa.interop.commons.utils.errors.GenericComponentErrors.ValidationRequestError
import it.pagopa.interop.commons.utils.{CORSSupport, OpenapiUtils}
import it.pagopa.interop.commons.vault.service.VaultService
import it.pagopa.interop.commons.vault.service.impl.{DefaultVaultClient, DefaultVaultService}
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
import it.pagopa.interop.notifier.server.Controller
import it.pagopa.interop.notifier.service._
import it.pagopa.interop.notifier.service.impl._
import it.pagopa.interop.purposemanagement.model.persistence.PurposeEventsSerde.jsonToPurpose
import kamon.Kamon

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext
import scala.util.Try

trait VaultServiceDependency {
  val vaultService: VaultService = new DefaultVaultService with DefaultVaultClient.DefaultClientInstance
}

trait SQSReaderDependency {
  implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(ApplicationConfiguration.threadPoolSize))
  val sqsReader   = QueueReader.get(ApplicationConfiguration.queueURL) {
    jsonToPurpose orElse jsonToAgreement
  }
}

object Main extends App with CORSSupport with VaultServiceDependency with SQSReaderDependency {

  val dependenciesLoaded: Try[(JWTReader, InteropTokenGenerator)] = for {
    keyset <- JWTConfiguration.jwtReader.loadKeyset()
    jwtReader             = new DefaultJWTReader with PublicKeysHolder {
      var publicKeyset: Map[KID, SerializedKey]                                        = keyset
      override protected val claimsVerifier: DefaultJWTClaimsVerifier[SecurityContext] =
        getClaimsVerifier(audience = ApplicationConfiguration.interopAudience)
    }
    interopTokenGenerator = new DefaultInteropTokenGenerator with PrivateKeysHolder {
      override val RSAPrivateKeyset: Map[KID, SerializedKey] =
        vaultService.readBase64EncodedData(ApplicationConfiguration.rsaPrivatePath)
      override val ECPrivateKeyset: Map[KID, SerializedKey]  =
        Map.empty
    }
  } yield (jwtReader, interopTokenGenerator)

  val (jwtReader, interopTokenGenerator) =
    dependenciesLoaded.get // THIS IS THE END OF THE WORLD. Exceptions are welcomed here.

  Kamon.init()

  lazy val notificationBehaviorFactory: EntityContext[Command] => Behavior[Command] = { entityContext =>
    OrganizationNotificationEventIdBehavior(
      entityContext.shard,
      PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId)
    )
  }

  locally {
    val _ = ActorSystem[Nothing](
      Behaviors.setup[Nothing] { context =>
        import akka.actor.typed.scaladsl.adapter._
        implicit val classicSystem: classic.ActorSystem = context.system.toClassic

        val cluster = Cluster(context.system)

        context.log.info(
          "Started [" + context.system + "], cluster.selfAddress = " + cluster.selfMember.address + ", build information = " + buildinfo.BuildInfo.toString + ")"
        )

        val sharding: ClusterSharding = ClusterSharding(context.system)

        val organizationNotificationEntity: Entity[Command, ShardingEnvelope[Command]] =
          Entity(OrganizationNotificationEventIdBehavior.TypeKey)(notificationBehaviorFactory)

        val _ = sharding.init(organizationNotificationEntity)

        val _ = AkkaManagement.get(classicSystem).start()

        val healthApi: HealthApi = new HealthApi(
          new HealthServiceApiImpl(),
          HealthApiMarshallerImpl,
          SecurityDirectives.authenticateOAuth2("SecurityRealm", PassThroughAuthenticator)
        )

        val dynamoReader =
          new DynamoServiceImpl(QueueConfiguration.queueAccountInfo, ApplicationConfiguration.dynamoTableName)

        val eventsApi: EventsApi =
          new EventsApi(
            new EventsServiceApiImpl(dynamoReader),
            EventsApiMarshallerImpl,
            jwtReader.OAuth2JWTValidatorAsContexts
          )

        val controller: Controller = new Controller(
          eventsApi,
          healthApi,
          validationExceptionToRoute = Some(report => {
            val error =
              problemOf(
                StatusCodes.BadRequest,
                ValidationRequestError(OpenapiUtils.errorFromRequestValidationReport(report))
              )
            complete(error.status, error)(HealthApiMarshallerImpl.toEntityMarshallerProblem)
          })
        )

        val catalogManagementService: CatalogManagementService =
          CatalogManagementServiceImpl(
            CatalogManagementInvoker(),
            CatalogManagementApi(ApplicationConfiguration.catalogManagementURL)
          )

        val agreementManagementService: AgreementManagementService =
          AgreementManagementServiceImpl(
            AgreementManagementInvoker(),
            AgreementManagementApi(ApplicationConfiguration.agreementManagementURL)
          )

        val purposeManagementService: PurposeManagementService =
          PurposeManagementServiceImpl(
            PurposeManagementInvoker(),
            PurposeManagementApi(ApplicationConfiguration.purposeManagementURL)
          )

        val eventIdRetriever =
          new EventIdRetriever(system = context.system, sharding = sharding, entity = organizationNotificationEntity)

        val handler =
          new QueueHandler(
            interopTokenGenerator,
            eventIdRetriever,
            dynamoReader,
            catalogManagementService,
            purposeManagementService,
            agreementManagementService
          )
        val _       = sqsReader.handle(handler.processMessage)

        val _ =
          Http().newServerAt("0.0.0.0", ApplicationConfiguration.serverPort).bind(controller.routes)

        val listener = context.spawn(
          Behaviors.receive[ClusterEvent.MemberEvent]((ctx, event) => {
            ctx.log.info("MemberEvent: {}", event)
            Behaviors.same
          }),
          "listener"
        )

        Cluster(context.system).subscriptions ! Subscribe(listener, classOf[ClusterEvent.MemberEvent])

        ClusterBootstrap.get(classicSystem).start()
        Behaviors.empty
      },
      "interop-be-notifier"
    )
  }
}
