package it.pagopa.interop.notifier.service.converters
import it.pagopa.interop.agreementmanagement.model.persistence._
import it.pagopa.interop.commons.queue.message.ProjectableEvent
import it.pagopa.interop.commons.utils.errors.ComponentError
import it.pagopa.interop.notifier.model.{AgreementPayload, MessageId, NotificationObjectType, NotificationPayload}
import it.pagopa.interop.notifier.service.impl.DynamoNotificationResourcesService

import scala.concurrent.{ExecutionContext, Future}

object AgreementEventsConverter {

  def getMessageId(dynamoService: DynamoNotificationResourcesService)(implicit
    ec: ExecutionContext,
    contexts: Seq[(String, String)]
  ): PartialFunction[ProjectableEvent, Future[MessageId]] = { case e: Event =>
    getMessageIdFromEvent(dynamoService, e)
  }

  private[this] def getMessageIdFromEvent(dynamoService: DynamoNotificationResourcesService, event: Event)(implicit
    ec: ExecutionContext,
    contexts: Seq[(String, String)]
  ): Future[MessageId] = event match {
    case AgreementAdded(a)                       =>
      val messageId: MessageId = MessageId(a.id, a.producerId.toString())
      dynamoService.put(messageId).map(_ => messageId)
    case AgreementDeleted(id)                    => getMessageIdFromDynamo(dynamoService)(id)
    case AgreementUpdated(a)                     => Future.successful(MessageId(a.id, a.producerId.toString()))
    case AgreementConsumerDocumentAdded(id, _)   => getMessageIdFromDynamo(dynamoService)(id)
    case AgreementConsumerDocumentRemoved(id, _) => getMessageIdFromDynamo(dynamoService)(id)
    case VerifiedAttributeUpdated(a)             => Future.successful(MessageId(a.id, a.producerId.toString()))
    case AgreementActivated(a)                   => Future.successful(MessageId(a.id, a.producerId.toString()))
    case AgreementSuspended(a)                   => Future.successful(MessageId(a.id, a.producerId.toString()))
    case AgreementDeactivated(a)                 => Future.successful(MessageId(a.id, a.producerId.toString()))
    case AgreementContractAdded(id, _)           => getMessageIdFromDynamo(dynamoService)(id)
  }

  def asNotificationPayload: PartialFunction[ProjectableEvent, Either[ComponentError, NotificationPayload]] = {
    case e: Event =>
      Right(getEventNotificationPayload(e))
  }

  private[this] def getEventNotificationPayload(event: Event): NotificationPayload = event match {
    case AgreementAdded(a)       =>
      AgreementPayload(agreementId = a.id.toString(), eventType = EventType.ADDED.toString())
    case AgreementUpdated(a)     =>
      AgreementPayload(agreementId = a.id.toString(), eventType = EventType.UPDATED.toString())
    case AgreementDeleted(id)    => AgreementPayload(agreementId = id, eventType = EventType.DELETED.toString())
    case AgreementActivated(a)   =>
      AgreementPayload(agreementId = a.id.toString(), eventType = EventType.UPDATED.toString())
    case AgreementSuspended(a)   =>
      AgreementPayload(agreementId = a.id.toString(), eventType = EventType.UPDATED.toString())
    case AgreementDeactivated(a) =>
      AgreementPayload(agreementId = a.id.toString(), eventType = EventType.UPDATED.toString())
    case AgreementConsumerDocumentAdded(id, _)   =>
      AgreementPayload(agreementId = id, eventType = EventType.UPDATED.toString())
    case AgreementConsumerDocumentRemoved(id, _) =>
      AgreementPayload(agreementId = id, eventType = EventType.UPDATED.toString())
    case VerifiedAttributeUpdated(a)             =>
      AgreementPayload(
        agreementId = a.id.toString(),
        eventType = EventType.UPDATED.toString(),
        objectType = NotificationObjectType.AGREEMENT_VERIFIED_ATTRIBUTE
      )
    case AgreementContractAdded(id, _)           =>
      AgreementPayload(agreementId = id, eventType = EventType.UPDATED.toString())

  }

}
