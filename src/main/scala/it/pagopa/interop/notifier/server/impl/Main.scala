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
import it.pagopa.interop.commons.jwt._
import it.pagopa.interop.commons.jwt.service.impl.{DefaultInteropTokenGenerator, DefaultJWTReader, getClaimsVerifier}
import it.pagopa.interop.commons.jwt.service.{InteropTokenGenerator, JWTReader}
import it.pagopa.interop.commons.utils.AkkaUtils.PassThroughAuthenticator
import it.pagopa.interop.commons.utils.errors.GenericComponentErrors.ValidationRequestError
import it.pagopa.interop.commons.utils.{CORSSupport, OpenapiUtils}
import it.pagopa.interop.commons.vault.service.VaultService
import it.pagopa.interop.commons.vault.service.impl.{DefaultVaultClient, DefaultVaultService}
import it.pagopa.interop.notifier.api.impl.{
  HealthApiMarshallerImpl,
  HealthServiceApiImpl,
  RegistryApiMarshallerImpl,
  RegistryServiceApiImpl,
  problemOf
}
import it.pagopa.interop.notifier.api.{HealthApi, RegistryApi}
import it.pagopa.interop.notifier.common.system.ApplicationConfiguration
import it.pagopa.interop.notifier.model.persistence.{
  OrganizationCommand,
  OrganizationNotificationCommand,
  OrganizationNotificationEventIdBehavior,
  OrganizationPersistentBehavior
}
import it.pagopa.interop.notifier.server.Controller
import kamon.Kamon

import scala.concurrent.ExecutionContext
import scala.util.Try

trait VaultServiceDependency {
  val vaultService: VaultService = new DefaultVaultService with DefaultVaultClient.DefaultClientInstance
}

object Main extends App with CORSSupport with VaultServiceDependency {

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

  lazy val behaviorFactory: EntityContext[OrganizationCommand] => Behavior[OrganizationCommand] = { entityContext =>
    OrganizationPersistentBehavior(
      entityContext.shard,
      PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId)
    )
  }

  lazy val notificationBehaviorFactory
    : EntityContext[OrganizationNotificationCommand] => Behavior[OrganizationNotificationCommand] = { entityContext =>
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
        implicit val executionContext: ExecutionContext = context.system.executionContext

        val cluster = Cluster(context.system)

        context.log.info(
          "Started [" + context.system + "], cluster.selfAddress = " + cluster.selfMember.address + ", build information = " + buildinfo.BuildInfo.toString + ")"
        )

        val sharding: ClusterSharding = ClusterSharding(context.system)

        val organizationPersistentEntity: Entity[OrganizationCommand, ShardingEnvelope[OrganizationCommand]] =
          Entity(OrganizationPersistentBehavior.TypeKey)(behaviorFactory)

        val organizationNotificationEntity
          : Entity[OrganizationNotificationCommand, ShardingEnvelope[OrganizationNotificationCommand]] =
          Entity(OrganizationNotificationEventIdBehavior.TypeKey)(notificationBehaviorFactory)

        val _ = sharding.init(organizationPersistentEntity)
        val _ = sharding.init(organizationNotificationEntity)

        val registryApi: RegistryApi =
          new RegistryApi(
            new RegistryServiceApiImpl(context.system, sharding, organizationPersistentEntity),
            RegistryApiMarshallerImpl,
            jwtReader.OAuth2JWTValidatorAsContexts
          )

        val healthApi: HealthApi = new HealthApi(
          new HealthServiceApiImpl(),
          HealthApiMarshallerImpl,
          SecurityDirectives.authenticateOAuth2("SecurityRealm", PassThroughAuthenticator)
        )

        locally {
          val _ = AkkaManagement.get(classicSystem).start()
        }

        val controller: Controller = new Controller(
          healthApi,
          registryApi,
          validationExceptionToRoute = Some(report => {
            val error =
              problemOf(
                StatusCodes.BadRequest,
                ValidationRequestError(OpenapiUtils.errorFromRequestValidationReport(report))
              )
            complete(error.status, error)(HealthApiMarshallerImpl.toEntityMarshallerProblem)
          })
        )

        val _ = Http().newServerAt("0.0.0.0", ApplicationConfiguration.serverPort).bind(controller.routes)

        val listener = context.spawn(
          Behaviors.receive[ClusterEvent.MemberEvent]((ctx, event) => {
            ctx.log.info("MemberEvent: {}", event)
            Behaviors.same
          }),
          "listener"
        )

        Cluster(context.system).subscriptions ! Subscribe(listener, classOf[ClusterEvent.MemberEvent])

        val _ = AkkaManagement(classicSystem).start()
        ClusterBootstrap.get(classicSystem).start()
        Behaviors.empty
      },
      "interop-be-authorization-management"
    )
  }
}
