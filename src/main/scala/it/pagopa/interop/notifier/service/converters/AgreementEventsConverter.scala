package it.pagopa.interop.notifier.service.converters
import it.pagopa.interop.agreementmanagement.model.persistence._
import it.pagopa.interop.commons.queue.message.ProjectableEvent
import it.pagopa.interop.commons.utils.errors.ComponentError
import it.pagopa.interop.notifier.model.{AgreementEventPayload, DynamoEventPayload}

import java.util.UUID
import scala.concurrent.Future

object AgreementEventsConverter {

  def getRecipient: PartialFunction[ProjectableEvent, Future[UUID]] = { case e: Event =>
    Future.successful(getEventRecipient(e))
  }

  private[this] def getEventRecipient(event: Event): UUID = event match {
    case VerifiedAttributeUpdated(a) => a.producerId
    case AgreementAdded(a)           => a.producerId
    case AgreementActivated(a)       => a.producerId
    case AgreementSuspended(a)       => a.producerId
    case AgreementDeactivated(a)     => a.producerId
  }

  def asDynamoPayload: PartialFunction[ProjectableEvent, Either[ComponentError, DynamoEventPayload]] = {
    case e: Event => getEventPayload(e)
  }

  private[this] def getEventPayload(event: Event): Either[ComponentError, DynamoEventPayload] = event match {
    case VerifiedAttributeUpdated(a) =>
      Right(
        AgreementEventPayload(
          agreementId = a.id.toString,
          eventType = EventType.UPDATED.toString,
          objectType = "AGREEMENT_VERIFIED_ATTRIBUTE"
        )
      )
    case AgreementAdded(a)           =>
      Right(AgreementEventPayload(agreementId = a.id.toString, eventType = EventType.ADDED.toString))
    case AgreementActivated(a)       =>
      Right(AgreementEventPayload(agreementId = a.id.toString, eventType = EventType.ACTIVATED.toString))
    case AgreementSuspended(a)       =>
      Right(AgreementEventPayload(agreementId = a.id.toString, eventType = EventType.SUSPENDED.toString))
    case AgreementDeactivated(a)     =>
      Right(AgreementEventPayload(agreementId = a.id.toString, eventType = EventType.DEACTIVATED.toString))
  }

}
