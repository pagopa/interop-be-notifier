package it.pagopa.interop.notifier.service.converters
import it.pagopa.interop.agreementmanagement.model.persistence._
import it.pagopa.interop.commons.queue.message.ProjectableEvent
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.commons.utils.errors.ComponentError
import it.pagopa.interop.notifier.model.persistence.MessageId
import it.pagopa.interop.notifier.model.{AgreementEventPayload, DynamoEventPayload}
import it.pagopa.interop.notifier.service.DynamoService

import scala.concurrent.{ExecutionContext, Future}

object AgreementEventsConverter {

  def getMessageId(dynamoService: DynamoService)(implicit
    ec: ExecutionContext,
    contexts: Seq[(String, String)]
  ): PartialFunction[ProjectableEvent, Future[MessageId]] = { case e: Event =>
    getMessageId(dynamoService, e)
  }

  private[this] def getMessageId(dynamoService: DynamoService, event: Event)(implicit
    ec: ExecutionContext,
    contexts: Seq[(String, String)]
  ): Future[MessageId] = event match {
    case AgreementAdded(a)                       => Future.successful(MessageId(a.id, a.producerId))
    case AgreementDeleted(id)                    => id.toFutureUUID.flatMap(createMessageId(dynamoService))
    case AgreementUpdated(a)                     => Future.successful(MessageId(a.id, a.producerId))
    case AgreementConsumerDocumentAdded(id, _)   => id.toFutureUUID.flatMap(createMessageId(dynamoService))
    case AgreementConsumerDocumentRemoved(id, _) => id.toFutureUUID.flatMap(createMessageId(dynamoService))
    case VerifiedAttributeUpdated(a)             => Future.successful(MessageId(a.id, a.producerId))
    case AgreementActivated(a)                   => Future.successful(MessageId(a.id, a.producerId))
    case AgreementSuspended(a)                   => Future.successful(MessageId(a.id, a.producerId))
    case AgreementDeactivated(a)                 => Future.successful(MessageId(a.id, a.producerId))
  }

  def asDynamoPayload: PartialFunction[ProjectableEvent, Either[ComponentError, DynamoEventPayload]] = {
    case e: Event => getEventPayload(e)
  }

  private[this] def getEventPayload(event: Event): Either[ComponentError, DynamoEventPayload] = event match {
    case AgreementAdded(a)     =>
      Right(AgreementEventPayload(agreementId = a.id.toString, eventType = EventType.ADDED.toString))
    case AgreementUpdated(a)   =>
      Right(AgreementEventPayload(agreementId = a.id.toString, eventType = EventType.UPDATED.toString))
    case AgreementDeleted(id)  => Right(AgreementEventPayload(agreementId = id, eventType = EventType.DELETED.toString))
    case AgreementActivated(a) =>
      Right(AgreementEventPayload(agreementId = a.id.toString, eventType = EventType.UPDATED.toString))
    case AgreementSuspended(a) =>
      Right(AgreementEventPayload(agreementId = a.id.toString, eventType = EventType.UPDATED.toString))
    case AgreementDeactivated(a)                 =>
      Right(AgreementEventPayload(agreementId = a.id.toString, eventType = EventType.UPDATED.toString))
    case AgreementConsumerDocumentAdded(id, _)   =>
      Right(AgreementEventPayload(agreementId = id, eventType = EventType.UPDATED.toString))
    case AgreementConsumerDocumentRemoved(id, _) =>
      Right(AgreementEventPayload(agreementId = id, eventType = EventType.UPDATED.toString))
    case VerifiedAttributeUpdated(a)             =>
      Right(
        AgreementEventPayload(
          agreementId = a.id.toString,
          eventType = EventType.UPDATED.toString,
          objectType = "AGREEMENT_VERIFIED_ATTRIBUTE"
        )
      )

  }

}
