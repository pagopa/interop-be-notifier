package it.pagopa.interop.notifier.service.converters
import it.pagopa.interop.agreementmanagement.model.persistence._
import it.pagopa.interop.commons.queue.message.ProjectableEvent
import it.pagopa.interop.commons.utils.errors.ComponentError
import it.pagopa.interop.notifier.model.{AgreementEventPayload, DynamoEventPayload}

import java.util.UUID
import scala.concurrent.Future

object AgreementEventsConverter {

  def getRecipient: PartialFunction[ProjectableEvent, Future[UUID]] = {
    case VerifiedAttributeUpdated(a) => Future.successful(a.producerId)
    case AgreementAdded(a)           => Future.successful(a.producerId)
    case AgreementActivated(a)       => Future.successful(a.producerId)
    case AgreementSuspended(a)       => Future.successful(a.producerId)
    case AgreementDeactivated(a)     => Future.successful(a.producerId)
  }

  def asDynamoPayload: PartialFunction[ProjectableEvent, Either[ComponentError, DynamoEventPayload]] = {
    case VerifiedAttributeUpdated(a) =>
      Right(
        AgreementEventPayload(
          agreementId = a.id.toString,
          eventType = UPDATED,
          objectType = "AGREEMENT_VERIFIED_ATTRIBUTE"
        )
      )
    case AgreementAdded(a)           => Right(AgreementEventPayload(agreementId = a.id.toString, eventType = ADDED))
    case AgreementActivated(a)       =>
      Right(AgreementEventPayload(agreementId = a.id.toString, eventType = ACTIVATED))
    case AgreementSuspended(a)       =>
      Right(AgreementEventPayload(agreementId = a.id.toString, eventType = SUSPENDED))
    case AgreementDeactivated(a)     =>
      Right(AgreementEventPayload(agreementId = a.id.toString, eventType = DEACTIVATED))
  }
}
