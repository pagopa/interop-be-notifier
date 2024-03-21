package it.pagopa.interop.notifier.service.converters
import it.pagopa.interop.agreementmanagement.model.persistence._
import it.pagopa.interop.commons.queue.message.ProjectableEvent
import it.pagopa.interop.commons.utils.errors.ComponentError
import it.pagopa.interop.notifier.model.{AgreementPayload, MessageId, NotificationObjectType, NotificationPayload}
import it.pagopa.interop.notifier.service.impl.DynamoNotificationResourcesService
import cats.syntax.all._
import it.pagopa.interop.agreementmanagement.model.agreement.Draft
import it.pagopa.interop.notifier.service.converters.agreementsPartition

import scala.concurrent.{ExecutionContext, Future}

object AgreementEventsConverter {

  def getMessageId(dynamoService: DynamoNotificationResourcesService)(implicit
    ec: ExecutionContext,
    contexts: Seq[(String, String)]
  ): PartialFunction[ProjectableEvent, Future[Option[MessageId]]] = { case e: Event =>
    getMessageIdFromEvent(dynamoService, e)
  }

  private[this] def getMessageIdFromEvent(dynamoService: DynamoNotificationResourcesService, event: Event)(implicit
    ec: ExecutionContext,
    contexts: Seq[(String, String)]
  ): Future[Option[MessageId]] = event match {
    case AgreementAdded(a)                       =>
      val messageId: MessageId = MessageId(a.id, Seq(a.producerId.toString(), agreementsPartition).mkString(separator))
      dynamoService.put(messageId).map(_ => messageId.some)
    case AgreementDeleted(id)                    => getMessageIdFromDynamo(dynamoService)(id).map(_.some)
    case AgreementUpdated(a)                     =>
      Future.successful(MessageId(a.id, Seq(a.producerId.toString(), agreementsPartition).mkString(separator)).some)
    case AgreementConsumerDocumentAdded(id, _)   => getMessageIdFromDynamo(dynamoService)(id).map(_.some)
    case AgreementConsumerDocumentRemoved(id, _) => getMessageIdFromDynamo(dynamoService)(id).map(_.some)
    case VerifiedAttributeUpdated(a)             =>
      Future.successful(MessageId(a.id, Seq(a.producerId.toString(), agreementsPartition).mkString(separator)).some)
    case AgreementActivated(a)                   =>
      Future.successful(MessageId(a.id, Seq(a.producerId.toString(), agreementsPartition).mkString(separator)).some)
    case AgreementSuspended(a)                   =>
      Future.successful(MessageId(a.id, Seq(a.producerId.toString(), agreementsPartition).mkString(separator)).some)
    case AgreementDeactivated(a)                 =>
      Future.successful(MessageId(a.id, Seq(a.producerId.toString(), agreementsPartition).mkString(separator)).some)
    case AgreementContractAdded(id, _)           => getMessageIdFromDynamo(dynamoService)(id).map(_.some)
  }

  def asNotificationPayload: PartialFunction[ProjectableEvent, Either[ComponentError, Option[NotificationPayload]]] = {
    case e: Event =>
      Right(getEventNotificationPayload(e))
  }

  private[this] def getEventNotificationPayload(event: Event): Option[NotificationPayload] = event match {
    // This event is emitted both on Draft creation and upgrade.
    //  Draft creation should not be notified
    case AgreementAdded(a) if a.state != Draft   =>
      AgreementPayload(agreementId = a.id.toString(), eventType = EventType.ADDED.toString()).some
    case AgreementUpdated(a)                     =>
      AgreementPayload(agreementId = a.id.toString(), eventType = EventType.UPDATED.toString()).some
    // Only agreements that have never been active can be deleted
    case _: AgreementDeleted                     => None
    case AgreementActivated(a)                   =>
      // Never used
      AgreementPayload(agreementId = a.id.toString(), eventType = EventType.UPDATED.toString()).some
    case AgreementSuspended(a)                   =>
      // Never used
      AgreementPayload(agreementId = a.id.toString(), eventType = EventType.UPDATED.toString()).some
    case AgreementDeactivated(a)                 =>
      // Never used
      AgreementPayload(agreementId = a.id.toString(), eventType = EventType.UPDATED.toString()).some
    case AgreementConsumerDocumentAdded(id, _)   =>
      // This operation can be done both on Draft and on Pending agreements.
      // Ideally we should notify just the latter
      AgreementPayload(agreementId = id, eventType = EventType.UPDATED.toString()).some
    case AgreementConsumerDocumentRemoved(id, _) =>
      // This operation can be done both on Draft and on Pending agreements.
      // Ideally we should notify just the latter
      AgreementPayload(agreementId = id, eventType = EventType.UPDATED.toString()).some
    case VerifiedAttributeUpdated(a)             =>
      // Never used
      AgreementPayload(
        agreementId = a.id.toString(),
        eventType = EventType.UPDATED.toString(),
        objectType = NotificationObjectType.AGREEMENT_VERIFIED_ATTRIBUTE
      ).some
    case AgreementContractAdded(id, _)           =>
      // We could identify this event as the creation of the agreement
      AgreementPayload(agreementId = id, eventType = EventType.ADDED.toString()).some
  }

}
