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
  CatalogEventsConverter,
  PurposeEventsConverter
}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import it.pagopa.interop.notifier.service.converters.notFoundRecipient

final class QueueHandler(
  interopTokenGenerator: InteropTokenGenerator,
  idRetriever: EventIdRetriever,
  dynamoNotificationService: DynamoNotificationService,
  dynamoIndexService: DynamoNotificationResourcesService,
  catalogManagementService: CatalogManagementService
)(implicit ec: ExecutionContext) {

  lazy val jwtConfig: JWTInternalTokenConfig = JWTConfiguration.jwtInternalTokenConfig
  private val logger: Logger                 = Logger(this.getClass)

  private def generateM2mContexts: Future[List[(String, String)]] = interopTokenGenerator
    .generateInternalToken(
      subject = jwtConfig.subject,
      audience = jwtConfig.audience.toList,
      tokenIssuer = jwtConfig.issuer,
      secondsDuration = jwtConfig.durationInSeconds
    )
    .map(m2mToken => List(CORRELATION_ID_HEADER -> UUID.randomUUID().toString, BEARER -> m2mToken.serialized))

  def processMessage(msg: Message): Future[Unit] = generateM2mContexts.flatMap(messageFlow(_, msg))

  private def messageFlow(contexts: Seq[(String, String)], message: Message): Future[Unit] =
    dynamoFlow(contexts)
      .orElse(postgreFlow(contexts))
      .applyOrElse(message, notFoundRecipient)

  // TODO

  private def postgreFlow: Seq[(String, String)] => PartialFunction[Message, Future[Unit]] = _ => { _: Message =>
    // TODO creare la tabella e salvare lo statement di create da qualche parte
    // Partial Function su ProjectableEvent deve diventare partial function su message
    // TODO m:Message => pf(m.payload): PF[Message, Future[Unit]]

    Future.unit
  }

  private def dynamoFlow: Seq[(String, String)] => PartialFunction[Message, Future[Unit]] = contexts => {
    msg: Message =>
      implicit val ctx: Seq[(String, String)] = contexts

      val getMessageId: PartialFunction[ProjectableEvent, Future[MessageId]] =
        PurposeEventsConverter.getMessageId(catalogManagementService, dynamoIndexService) orElse
          AgreementEventsConverter.getMessageId(dynamoIndexService) orElse
          CatalogEventsConverter.getMessageId(dynamoIndexService)

      val flow: PartialFunction[ProjectableEvent, Future[Unit]] = getMessageId
        .andThen(messageIdF =>
          for {
            messageId <- messageIdF
            _ = logger.debug(
              s"Organization id retrieved for message  ${msg.messageUUID} -> ${messageId.organizationId}"
            )
            nextEvent <- idRetriever.getNextEventIdForOrganization(messageId.organizationId)(contexts)
            _ = logger.debug(s"Next event id for organization ${nextEvent.organizationId} -> ${nextEvent.eventId}")
            notificationMessage <- NotificationMessage.create(messageId, nextEvent.eventId, msg).toFuture
            ()                  <- dynamoNotificationService.put(notificationMessage)
            _ = logger.debug(s"Message ${msg.messageUUID.toString} was successfully written to dynamodb")
          } yield ()
        )

      flow(msg.payload)
  }

}
