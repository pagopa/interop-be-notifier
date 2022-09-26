package it.pagopa.interop.notifier.service.impl

import com.typesafe.scalalogging.Logger
import it.pagopa.interop.commons.jwt.service.InteropTokenGenerator
import it.pagopa.interop.commons.jwt.{JWTConfiguration, JWTInternalTokenConfig}
import it.pagopa.interop.commons.queue.message.{Message, ProjectableEvent}
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.commons.utils.{BEARER, CORRELATION_ID_HEADER}
import it.pagopa.interop.notifier.model.{MessageId, NotificationMessage}
import it.pagopa.interop.notifier.service.CatalogManagementService
import it.pagopa.interop.notifier.service.converters.{
  AgreementEventsConverter,
  PurposeEventsConverter,
  notFoundRecipient
}
import org.scanamo.ScanamoAsync

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

final class QueueHandler(
  interopTokenGenerator: InteropTokenGenerator,
  idRetriever: EventIdRetriever,
  dynamoNotificationService: DynamoNotificationService,
  dynamoIndexService: DynamoIndexService,
  catalogManagementService: CatalogManagementService
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
  def processMessage(msg: Message)(implicit scanamo: ScanamoAsync): Future[Unit] = for {
    m2mToken <- interopTokenGenerator
      .generateInternalToken(
        subject = jwtConfig.subject,
        audience = jwtConfig.audience.toList,
        tokenIssuer = jwtConfig.issuer,
        secondsDuration = jwtConfig.durationInSeconds
      )
    m2mContexts = Seq(CORRELATION_ID_HEADER -> UUID.randomUUID().toString, BEARER -> m2mToken.serialized)
    messageId <- extractMessageId(msg.payload, dynamoIndexService)(scanamo, m2mContexts)
    _ = logger.debug(s"Organization id retrieved for message  ${msg.messageUUID} -> ${messageId.organizationId}")
    nextEvent <- idRetriever.getNextEventIdForOrganization(messageId.organizationId)(m2mContexts)
    _ = logger.debug(s"Next event id for organization ${nextEvent.organizationId} -> ${nextEvent.eventId}")
    notificationMessage <- NotificationMessage.create(messageId, nextEvent.eventId, msg).toFuture
    result              <- dynamoNotificationService.put(notificationMessage)
    _ = logger.debug(s"Message ${msg.messageUUID.toString} was successfully written to dynamodb")
  } yield result

  private[this] def extractMessageId(event: ProjectableEvent, dynamoIndexService: DynamoIndexService)(implicit
    scanamo: ScanamoAsync,
    contexts: Seq[(String, String)]
  ): Future[MessageId] = {
    val composedGetters: PartialFunction[ProjectableEvent, Future[MessageId]] =
      PurposeEventsConverter.getMessageId(catalogManagementService, dynamoIndexService) orElse AgreementEventsConverter
        .getMessageId(dynamoIndexService) orElse notFoundRecipient

    composedGetters(event)
  }

}
