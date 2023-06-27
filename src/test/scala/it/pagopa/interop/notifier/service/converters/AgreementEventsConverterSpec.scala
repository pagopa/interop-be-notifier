package it.pagopa.interop.notifier.service.converters

import it.pagopa.interop.agreementmanagement.model.agreement.{Active, PersistentAgreement, PersistentStamps}
import it.pagopa.interop.agreementmanagement.model.persistence._
import it.pagopa.interop.commons.utils.errors.ComponentError
import it.pagopa.interop.notifier.model.{AgreementPayload, NotificationObjectType, NotificationPayload}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.time.OffsetDateTime
import java.util.UUID
import cats.syntax.all._

class AgreementEventsConverterSpec extends AnyWordSpecLike with Matchers with ScalaFutures {

  "Agreement conversions" should {

    "Convert purpose created to event payload" in {
      // given
      val id = UUID.randomUUID()
      val a  = getAgreement(id)
      val e  = AgreementAdded(a)

      // when
      val conversion: Either[ComponentError, Option[NotificationPayload]] =
        AgreementEventsConverter.asNotificationPayload(e)
      // then
      conversion shouldBe Right(AgreementPayload(id.toString, EventType.ADDED.toString).some)
    }

    "Convert purpose deleted to event payload" in {
      // given
      val id = UUID.randomUUID()
      val e  = AgreementDeleted(id.toString)

      // when
      val conversion: Either[ComponentError, Option[NotificationPayload]] =
        AgreementEventsConverter.asNotificationPayload(e)
      // then
      conversion shouldBe Right(AgreementPayload(id.toString, EventType.DELETED.toString).some)
    }

    "Convert purpose updated to event payload" in {
      // given
      val id = UUID.randomUUID()
      val a  = getAgreement(id)
      val e  = AgreementUpdated(a)

      // when
      val conversion: Either[ComponentError, Option[NotificationPayload]] =
        AgreementEventsConverter.asNotificationPayload(e)
      // then
      conversion shouldBe Right(AgreementPayload(id.toString, EventType.UPDATED.toString).some)
    }

    "Convert purpose activated to event payload" in {
      // given
      val id = UUID.randomUUID()
      val a  = getAgreement(id)
      val e  = AgreementActivated(a)

      // when
      val conversion: Either[ComponentError, Option[NotificationPayload]] =
        AgreementEventsConverter.asNotificationPayload(e)
      // then
      conversion shouldBe Right(AgreementPayload(id.toString, EventType.UPDATED.toString).some)
    }

    "Convert purpose suspended to event payload" in {
      // given
      val id = UUID.randomUUID()
      val a  = getAgreement(id)
      val e  = AgreementSuspended(a)

      // when
      val conversion: Either[ComponentError, Option[NotificationPayload]] =
        AgreementEventsConverter.asNotificationPayload(e)
      // then
      conversion shouldBe Right(AgreementPayload(id.toString, EventType.UPDATED.toString).some)
    }

    "Convert purpose deactivated to event payload" in {
      // given
      val id = UUID.randomUUID()
      val a  = getAgreement(id)
      val e  = AgreementDeactivated(a)

      // when
      val conversion: Either[ComponentError, Option[NotificationPayload]] =
        AgreementEventsConverter.asNotificationPayload(e)
      // then
      conversion shouldBe Right(AgreementPayload(id.toString, EventType.UPDATED.toString).some)
    }

    "Convert verified attribute updated to event payload" in {
      // given
      val id = UUID.randomUUID()
      val a  = getAgreement(id)
      val e  = VerifiedAttributeUpdated(a)

      // when
      val conversion: Either[ComponentError, Option[NotificationPayload]] =
        AgreementEventsConverter.asNotificationPayload(e)
      // then
      conversion shouldBe Right(
        AgreementPayload(
          id.toString,
          EventType.UPDATED.toString,
          objectType = NotificationObjectType.AGREEMENT_VERIFIED_ATTRIBUTE
        ).some
      )
    }

  }

  private def getAgreement(id: UUID) = PersistentAgreement(
    id = id,
    eserviceId = UUID.randomUUID(),
    descriptorId = UUID.randomUUID(),
    producerId = UUID.randomUUID(),
    consumerId = UUID.randomUUID(),
    state = Active,
    verifiedAttributes = List.empty,
    certifiedAttributes = List.empty,
    declaredAttributes = List.empty,
    suspendedByConsumer = None,
    suspendedByProducer = None,
    suspendedByPlatform = None,
    consumerDocuments = List.empty,
    createdAt = OffsetDateTime.now(),
    updatedAt = None,
    suspendedAt = None,
    consumerNotes = None,
    contract = None,
    stamps = PersistentStamps(),
    rejectionReason = None
  )

}
