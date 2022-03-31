package it.pagopa.interop.notifier.server.impl

import akka.actor.CoordinatedShutdown
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.directives.SecurityDirectives
import akka.management.scaladsl.AkkaManagement
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import it.pagopa.interop.commons.jwt._
import it.pagopa.interop.commons.jwt.service.impl.{DefaultInteropTokenGenerator, DefaultJWTReader, getClaimsVerifier}
import it.pagopa.interop.commons.jwt.service.{InteropTokenGenerator, JWTReader}
import it.pagopa.interop.commons.utils.AkkaUtils.PassThroughAuthenticator
import it.pagopa.interop.commons.utils.TypeConversions.TryOps
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
import it.pagopa.interop.notifier.common.ApplicationConfiguration
import it.pagopa.interop.notifier.common.system.{classicActorSystem, executionContext}
import it.pagopa.interop.notifier.server.Controller
import it.pagopa.interop.notifier.service.impl.MongoDBPersistentService
import kamon.Kamon

import scala.annotation.nowarn
import scala.concurrent.Future
import scala.util.{Failure, Success}
//shuts down the actor system in case of startup errors
case object StartupErrorShutdown extends CoordinatedShutdown.Reason

trait VaultServiceDependency {
  val vaultService: VaultService = new DefaultVaultService with DefaultVaultClient.DefaultClientInstance
}

object Main extends App with CORSSupport with VaultServiceDependency {

  val dependenciesLoaded: Future[(JWTReader, InteropTokenGenerator)] = for {
    keyset <- JWTConfiguration.jwtReader.loadKeyset().toFuture
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

  dependenciesLoaded.transformWith {
    case Success((jwtReader, interopTokenGenerator)) => launchApp(jwtReader, interopTokenGenerator)
    case Failure(ex)                                 =>
      classicActorSystem.log.error(s"Startup error: ${ex.getMessage}")
      classicActorSystem.log.error(s"${ex.getStackTrace.mkString("\n")}")
      CoordinatedShutdown(classicActorSystem).run(StartupErrorShutdown)
  }

  private def launchApp(
    jwtReader: JWTReader,
    @nowarn interopTokenGenerator: InteropTokenGenerator
  ): Future[Http.ServerBinding] = {
    Kamon.init()

    val registryApi: RegistryApi =
      new RegistryApi(
        new RegistryServiceApiImpl(new MongoDBPersistentService(ApplicationConfiguration.dbConfiguration)),
        RegistryApiMarshallerImpl,
        jwtReader.OAuth2JWTValidatorAsContexts
      )

    val healthApi: HealthApi = new HealthApi(
      new HealthServiceApiImpl(),
      HealthApiMarshallerImpl,
      SecurityDirectives.authenticateOAuth2("SecurityRealm", PassThroughAuthenticator)
    )

    locally {
      val _ = AkkaManagement.get(classicActorSystem).start()
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

    val server: Future[Http.ServerBinding] =
      Http().newServerAt("0.0.0.0", ApplicationConfiguration.serverPort).bind(corsHandler(controller.routes))

    server
  }
}
