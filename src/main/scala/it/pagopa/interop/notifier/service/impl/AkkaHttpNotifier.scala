package it.pagopa.interop.notifier.service.impl

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import it.pagopa.interop.commons.jwt.model.RSA
import it.pagopa.interop.commons.jwt.service.InteropTokenGenerator
import it.pagopa.interop.commons.jwt.{JWTConfiguration, JWTInternalTokenConfig}
import it.pagopa.interop.commons.utils.TypeConversions.TryOps
import it.pagopa.interop.notifier.model.{Notification, Organization}
import it.pagopa.interop.notifier.service.EventNotifier

import scala.concurrent.{ExecutionContext, Future}

/**
  * Implements event notification sender with Akka-HTTP
  * @param interopTokenGenerator external dependency to forge the JWT used for the communication with organization URI
  * @param system actor system used by Akka HTTP
  */
class AkkaHttpNotifier(interopTokenGenerator: InteropTokenGenerator)(implicit system: ActorSystem[Nothing])
    extends EventNotifier {

  lazy val jwtConfig: JWTInternalTokenConfig = JWTConfiguration.jwtInternalTokenConfig

  def notifyAndForget(notification: Notification, organization: Organization): Future[HttpResponse] = {
    implicit val executionContext = system.executionContext
    for {
      m2mToken <- forgeToken(organization.audience)
      response <- invokeOrganizationEndpoint(notification, organization.notificationURL, m2mToken)
    } yield response
  }

  private[this] def forgeToken(organizationAudience: String): Future[String] = interopTokenGenerator
    .generateInternalToken(
      jwtAlgorithmType = RSA,
      subject = jwtConfig.subject,
      audience = List(organizationAudience),
      tokenIssuer = jwtConfig.issuer,
      secondsDuration = jwtConfig.durationInSeconds
    )
    .toFuture

  private[this] def invokeOrganizationEndpoint(notification: Notification, organizationURI: String, token: String)(
    implicit ec: ExecutionContext
  ): Future[HttpResponse] = {
    val requestHeaders: Seq[HttpHeader] =
      Seq(headers.Authorization(OAuth2BearerToken(token)))
    for {
      data     <- Marshal(notification).to[MessageEntity].map(_.dataBytes)
      response <- Http().singleRequest(
        HttpRequest(
          uri = organizationURI,
          method = HttpMethods.POST,
          entity = HttpEntity(ContentTypes.`application/json`, data),
          headers = requestHeaders
        )
      )
    } yield response
  }

}
