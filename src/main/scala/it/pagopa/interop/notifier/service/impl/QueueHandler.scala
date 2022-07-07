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
import it.pagopa.interop.notifier.service.{CatalogManagementService, DynamoService, PurposeManagementService}
import com.typesafe.scalalogging.Logger

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class QueueHandler(
  val interopTokenGenerator: InteropTokenGenerator,
  val idRetriever: EventIdRetriever,
  val dynamoService: DynamoService,
  val catalogManagementService: CatalogManagementService,
  val purposeManagementService: PurposeManagementService
)(implicit ec: ExecutionContext) {

  lazy val jwtConfig: JWTInternalTokenConfig = JWTConfiguration.jwtInternalTokenConfig
  private val logger                         = Logger(this.getClass)

  /*
   * it does:
   *
   *  - M2M token generation for calling services downstream
   *  - given a message payload, retrieves the recipient of the message
   *  - gets the next event id for the corresponding recipient
   *  - saves the event on dynamo
   */
  def processMessage(msg: Message): Future[Unit] = for {
    m2mToken <- interopTokenGenerator
      .generateInternalToken(
        subject = jwtConfig.subject,
        audience = jwtConfig.audience.toList,
        tokenIssuer = jwtConfig.issuer,
        secondsDuration = jwtConfig.durationInSeconds
      )
    m2mContexts = Seq(CORRELATION_ID_HEADER -> UUID.randomUUID().toString, BEARER -> m2mToken.serialized)
    organizationId <- getRecipientId(msg.payload)(m2mContexts)
    _ = logger.debug("Organization id retrieved for message {} -> {}", msg.messageUUID, organizationId)
    nextEvent <- idRetriever.getNextEventIdForOrganization(organizationId)(m2mContexts)
    _ = logger.debug("Next event id for organization {} -> {}", nextEvent.organizationId, nextEvent.eventId)
    result <- dynamoService.put(organizationId, nextEvent.eventId, msg)
    _ = logger.debug("Message {} was successfully written to dynamodb", msg.messageUUID.toString)
  } yield result

  // gets the identifier of the recipient organization id
  private[this] def getRecipientId(
    message: ProjectableEvent
  )(implicit contexts: Seq[(String, String)]): Future[UUID] = {
    val composedGetters =
      PurposeEventsConverter.getRecipient(
        catalogManagementService,
        purposeManagementService
      ) orElse AgreementEventsConverter.getRecipient orElse notFoundRecipient

    composedGetters(message)
  }

}
