package it.pagopa.interop.notifier.service.converters
import it.pagopa.interop.agreementmanagement.model.persistence._
import it.pagopa.interop.commons.queue.message.ProjectableEvent
import it.pagopa.interop.commons.utils.errors.ComponentError
import it.pagopa.interop.notifier.model.{AgreementPayload, MessageId, NotificationObjectType, NotificationPayload}
import it.pagopa.interop.notifier.service.impl.DynamoNotificationResourcesService
import cats.syntax.all._

import scala.concurrent.{ExecutionContext, Future}

object AgreementEventsConverter {

  def getMessageId(dynamoService: DynamoNotificationResourcesService)(implicit
    ec: ExecutionContext,
    contexts: Seq[(String, String)]
  ): PartialFunction[ProjectableEvent, Future[Seq[MessageId]]] = { case e: Event =>
    getMessageIdFromEvent(dynamoService, e)
  }

  private[this] def getMessageIdFromEvent(dynamoService: DynamoNotificationResourcesService, event: Event)(implicit
    ec: ExecutionContext,
    contexts: Seq[(String, String)]
  ): Future[Seq[MessageId]] = event match {
    case AgreementAdded(a)                       => 
      val messageId = MessageId(a.id, a.producerId.toString())
      val messageAgreements = MessageId(a.id, agreements)
      for {
      _ <- dynamoService.put(messageId)
      _ <-  dynamoService.put(messageAgreements)
    } yield Seq(messageId, messageAgreements)
    case AgreementDeleted(id)                    => getMessageIdFromDynamo(dynamoService)(id).map(Seq(_))
    case AgreementUpdated(a)                     => Future.successful(Seq(MessageId(a.id, a.producerId.toString()), MessageId(a.id, agreements)))
    case AgreementConsumerDocumentAdded(id, _)   => getMessageIdFromDynamo(dynamoService)(id).map(Seq(_))
    case AgreementConsumerDocumentRemoved(id, _) => getMessageIdFromDynamo(dynamoService)(id).map(Seq(_))
    case VerifiedAttributeUpdated(a)             => Future.successful(Seq(MessageId(a.id, a.producerId.toString()), MessageId(a.id, agreements)))
    case AgreementActivated(a)                   => Future.successful(Seq(MessageId(a.id, a.producerId.toString()), MessageId(a.id, agreements)))
    case AgreementSuspended(a)                   => Future.successful(Seq(MessageId(a.id, a.producerId.toString()), MessageId(a.id, agreements)))
    case AgreementDeactivated(a)                 => Future.successful(Seq(MessageId(a.id, a.producerId.toString()), MessageId(a.id, agreements)))
    case AgreementContractAdded(id, _)           => getMessageIdFromDynamo(dynamoService)(id).map(Seq(_))
  }

  def asNotificationPayload: PartialFunction[ProjectableEvent, Either[ComponentError, Option[NotificationPayload]]] = {
    case e: Event =>
      Right(getEventNotificationPayload(e))
  }

  private[this] def getEventNotificationPayload(event: Event): Option[NotificationPayload] = event match {
    // Agreements are created with status Draft, and should not be notified
    case _: AgreementAdded                       => None
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
