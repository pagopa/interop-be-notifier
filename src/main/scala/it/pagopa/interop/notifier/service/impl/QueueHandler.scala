package it.pagopa.interop.notifier.service.impl

import com.typesafe.scalalogging.Logger
import it.pagopa.interop.commons.jwt.service.InteropTokenGenerator
import it.pagopa.interop.commons.jwt.{JWTConfiguration, JWTInternalTokenConfig}
import it.pagopa.interop.commons.queue.message.{Message, ProjectableEvent}
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.commons.utils.{BEARER, CORRELATION_ID_HEADER}
import it.pagopa.interop.notifier.model.{MessageId, NotificationMessage}
import it.pagopa.interop.notifier.service.converters.{
  AgreementEventsConverter,
  CatalogEventsConverter,
  PurposeEventsConverter,
  notFoundRecipient
}
import it.pagopa.interop.notifier.service.{AuthorizationEventsHandler, CatalogManagementService}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

final class QueueHandler(
  interopTokenGenerator: InteropTokenGenerator,
  idRetriever: EventIdRetriever,
  dynamoNotificationService: DynamoNotificationService,
  dynamoIndexService: DynamoNotificationResourcesService,
  catalogManagementService: CatalogManagementService,
  authorizationEventsHandler: AuthorizationEventsHandler
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
      .orElse(postgreFlow)
      .applyOrElse(message, notFoundRecipient)

  private def postgreFlow: PartialFunction[Message, Future[Unit]] = authorizationEventsHandler.handleEvents

  private def dynamoFlow: Seq[(String, String)] => PartialFunction[Message, Future[Unit]] = contexts =>
    Function.unlift({ msg: Message =>
      implicit val ctx: Seq[(String, String)] = contexts

      val getMessageId: PartialFunction[ProjectableEvent, Future[Option[MessageId]]] =
        PurposeEventsConverter.getMessageId(catalogManagementService, dynamoIndexService) orElse
          AgreementEventsConverter.getMessageId(dynamoIndexService) orElse
          CatalogEventsConverter.getMessageId(dynamoIndexService)

      val flow: PartialFunction[ProjectableEvent, Future[Unit]] = getMessageId
        .andThen(_.flatMap {
          case Some(messageId) =>
            logger.debug(s"Organization id retrieved for message  ${msg.messageUUID} -> ${messageId.organizationId}")
            for {
              nextEvent <- idRetriever.getNextEventIdForOrganization(messageId.organizationId)(contexts)
              _ = logger.debug(s"Next event id for organization ${nextEvent.organizationId} -> ${nextEvent.eventId}")
              notificationMessage <- NotificationMessage.create(messageId, nextEvent.eventId, msg).toFuture
              ()                  <- notificationMessage.fold(Future.unit)(dynamoNotificationService.put)
              _ = logger.debug(s"Message ${msg.messageUUID.toString} was successfully written to dynamodb")
            } yield ()
          case None            => Future.unit
        })

      flow.lift(msg.payload)
    })

}
