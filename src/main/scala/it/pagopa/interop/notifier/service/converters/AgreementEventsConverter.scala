package it.pagopa.interop.notifier.service.converters
import it.pagopa.interop.agreementmanagement.model.persistence._
import it.pagopa.interop.commons.queue.message.ProjectableEvent
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.commons.utils.errors.ComponentError
import it.pagopa.interop.notifier.model.{AgreementPayload, MessageId, NotificationPayload}
import it.pagopa.interop.notifier.service.impl.DynamoNotificationResourcesService
import org.scanamo.ScanamoAsync

import scala.concurrent.{ExecutionContext, Future}

object AgreementEventsConverter {

  def getMessageId(dynamoService: DynamoNotificationResourcesService)(implicit
    scanamo: ScanamoAsync,
    ec: ExecutionContext,
    contexts: Seq[(String, String)]
  ): PartialFunction[ProjectableEvent, Future[MessageId]] = { case e: Event =>
    getMessageIdFromEvent(dynamoService, e)
  }

  private[this] def getMessageIdFromEvent(dynamoService: DynamoNotificationResourcesService, event: Event)(implicit
    scanamo: ScanamoAsync,
    ec: ExecutionContext,
    contexts: Seq[(String, String)]
  ): Future[MessageId] = event match {
    case AgreementAdded(a)                       =>
      val messageId: MessageId = MessageId(a.id, a.producerId)
      dynamoService.put(messageId).map(_ => messageId)
    case AgreementDeleted(id)                    => id.toFutureUUID.flatMap(getMessageIdFromDynamo(dynamoService))
    case AgreementUpdated(a)                     => Future.successful(MessageId(a.id, a.producerId))
    case AgreementConsumerDocumentAdded(id, _)   => id.toFutureUUID.flatMap(getMessageIdFromDynamo(dynamoService))
    case AgreementConsumerDocumentRemoved(id, _) => id.toFutureUUID.flatMap(getMessageIdFromDynamo(dynamoService))
    case VerifiedAttributeUpdated(a)             => Future.successful(MessageId(a.id, a.producerId))
    case AgreementActivated(a)                   => Future.successful(MessageId(a.id, a.producerId))
    case AgreementSuspended(a)                   => Future.successful(MessageId(a.id, a.producerId))
    case AgreementDeactivated(a)                 => Future.successful(MessageId(a.id, a.producerId))
  }

  def asNotificationPayload: PartialFunction[ProjectableEvent, Either[ComponentError, NotificationPayload]] = {
    case e: Event =>
      Right(getEventNotificationPayload(e))
  }

  private[this] def getEventNotificationPayload(event: Event): NotificationPayload = event match {
    case AgreementAdded(a)       =>
      AgreementPayload(agreementId = a.id.toString, eventType = EventType.ADDED.toString)
    case AgreementUpdated(a)     =>
      AgreementPayload(agreementId = a.id.toString, eventType = EventType.UPDATED.toString)
    case AgreementDeleted(id)    => AgreementPayload(agreementId = id, eventType = EventType.DELETED.toString)
    case AgreementActivated(a)   =>
      AgreementPayload(agreementId = a.id.toString, eventType = EventType.UPDATED.toString)
    case AgreementSuspended(a)   =>
      AgreementPayload(agreementId = a.id.toString, eventType = EventType.UPDATED.toString)
    case AgreementDeactivated(a) =>
      AgreementPayload(agreementId = a.id.toString, eventType = EventType.UPDATED.toString)
    case AgreementConsumerDocumentAdded(id, _)   =>
      AgreementPayload(agreementId = id, eventType = EventType.UPDATED.toString)
    case AgreementConsumerDocumentRemoved(id, _) =>
      AgreementPayload(agreementId = id, eventType = EventType.UPDATED.toString)
    case VerifiedAttributeUpdated(a)             =>
      AgreementPayload(
        agreementId = a.id.toString,
        eventType = EventType.UPDATED.toString,
        objectType = "AGREEMENT_VERIFIED_ATTRIBUTE"
      )

  }

}
