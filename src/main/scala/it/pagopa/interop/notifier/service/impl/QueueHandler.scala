package it.pagopa.interop.notifier.service.impl

import it.pagopa.interop.commons.jwt.service.InteropTokenGenerator
import it.pagopa.interop.commons.jwt.{JWTConfiguration, JWTInternalTokenConfig}
import it.pagopa.interop.commons.queue.message.{Message, ProjectableEvent}
import it.pagopa.interop.commons.utils.{BEARER, CORRELATION_ID_HEADER}
import it.pagopa.interop.notifier.service.converters.{
  AgreementEventsConverter,
  PurposeEventsConverter,
  notFoundRecipient
}
import it.pagopa.interop.notifier.service.{
  AgreementManagementService,
  CatalogManagementService,
  DynamoService,
  PurposeManagementService
}
import org.slf4j.LoggerFactory

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class QueueHandler(
  val interopTokenGenerator: InteropTokenGenerator,
  val idRetriever: EventIdRetriever,
  val dynamoService: DynamoService,
  val catalogManagementService: CatalogManagementService,
  val purposeManagementService: PurposeManagementService,
  val agreementManagementService: AgreementManagementService
)(implicit ec: ExecutionContext) {

  lazy val jwtConfig: JWTInternalTokenConfig = JWTConfiguration.jwtInternalTokenConfig
  private val logger                         = LoggerFactory.getLogger(this.getClass)

  /*
   * it does:
   *
   *  - M2M token generation for calling services downstream
   *  - given a message payload, retrieves the recipient of the message
   *  - gets the next event id for the corresponding recipient
   *  - saves the event on dynamo
   */
  def processMessage(msg: Message): Future[Unit] = {
    for {
      m2mToken <- interopTokenGenerator
        .generateInternalToken(
          subject = jwtConfig.subject,
          audience = jwtConfig.audience.toList,
          tokenIssuer = jwtConfig.issuer,
          secondsDuration = jwtConfig.durationInSeconds
        )
      m2mContexts = Seq(CORRELATION_ID_HEADER -> UUID.randomUUID().toString, BEARER -> m2mToken.serialized)
      organizationId <- getRecipientId(m2mContexts)(msg.payload)
      _ = logger.debug("Organization id retrieved for message {} -> {}", msg.messageUUID, organizationId)
      nextEvent <- idRetriever.getNextEventIdForOrganization(organizationId)
      _ = logger.debug("Next event id for organization {} -> {}", nextEvent.organizationId, nextEvent.eventId)
      _ = dynamoService.put(organizationId, nextEvent.eventId, msg)
    } yield ()
  }

  // gets the identifier of the recipient organization id
  private[this] def getRecipientId(contexts: Seq[(String, String)])(message: ProjectableEvent): Future[UUID] = {
    val composedGetters =
      PurposeEventsConverter.getRecipient(
        catalogManagementService,
        purposeManagementService,
        contexts
      ) orElse AgreementEventsConverter.getRecipient orElse notFoundRecipient

    composedGetters(message)
  }

}
