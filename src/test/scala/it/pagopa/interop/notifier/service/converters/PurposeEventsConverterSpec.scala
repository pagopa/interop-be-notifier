package it.pagopa.interop.notifier.service.converters

import it.pagopa.interop.commons.utils.errors.ComponentError
import it.pagopa.interop.notifier.model.{NotificationPayload, PurposePayload}
import it.pagopa.interop.purposemanagement.model.persistence._
import it.pagopa.interop.purposemanagement.model.purpose.{Active, PersistentPurpose, PersistentPurposeVersion}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.time.OffsetDateTime
import java.util.UUID
import cats.syntax.all._

class PurposeEventsConverterSpec extends AnyWordSpecLike with Matchers {

  "Purpose conversions" should {

    "Convert purpose created to event payload" in {
      // given
      val id = UUID.randomUUID()
      val pp = getPurpose(id)
      val p  = PurposeCreated(pp)

      // when
      val conversion: Either[ComponentError, Option[NotificationPayload]] =
        PurposeEventsConverter.asNotificationPayload(p)
      // then
      conversion shouldBe Right(PurposePayload(id.toString, EventType.CREATED.toString).some)
    }

    "Convert purpose updated to event payload" in {
      // given
      val id = UUID.randomUUID()
      val pp = getPurpose(id)
      val p  = PurposeUpdated(pp)

      // when
      val conversion: Either[ComponentError, Option[NotificationPayload]] =
        PurposeEventsConverter.asNotificationPayload(p)
      // then
      conversion shouldBe Right(PurposePayload(id.toString, EventType.UPDATED.toString).some)
    }

    "Convert purpose version created to event payload" in {
      // given
      val id        = UUID.randomUUID()
      val versionId = UUID.randomUUID()
      val pv        = getPurposeVersion(versionId)
      val e         = PurposeVersionCreated(id.toString, pv)

      // when
      val conversion: Either[ComponentError, Option[NotificationPayload]] =
        PurposeEventsConverter.asNotificationPayload(e)
      // then
      conversion shouldBe Right(PurposePayload(id.toString, EventType.CREATED.toString).some)
    }

    "Convert purpose activated to event payload" in {
      // given
      val id = UUID.randomUUID()
      val pp = getPurpose(id)
      val e  = PurposeVersionActivated(pp)

      // when
      val conversion: Either[ComponentError, Option[NotificationPayload]] =
        PurposeEventsConverter.asNotificationPayload(e)
      // then
      conversion shouldBe Right(PurposePayload(id.toString, EventType.ACTIVATED.toString).some)
    }

    "Convert purpose suspended to event payload" in {
      // given
      val id = UUID.randomUUID()
      val pp = getPurpose(id)
      val e  = PurposeVersionSuspended(pp)

      // when
      val conversion: Either[ComponentError, Option[NotificationPayload]] =
        PurposeEventsConverter.asNotificationPayload(e)
      // then
      conversion shouldBe Right(PurposePayload(id.toString, EventType.SUSPENDED.toString).some)
    }

    "Convert purpose archived to event payload" in {
      // given
      val id = UUID.randomUUID()
      val pp = getPurpose(id)
      val e  = PurposeVersionArchived(pp)

      // when
      val conversion: Either[ComponentError, Option[NotificationPayload]] =
        PurposeEventsConverter.asNotificationPayload(e)
      // then
      conversion shouldBe Right(PurposePayload(id.toString, EventType.ARCHIVED.toString).some)
    }

    "Convert purpose version updated to event payload" in {
      // given
      val id        = UUID.randomUUID()
      val versionId = UUID.randomUUID()
      val pp        = getPurposeVersion(versionId)
      val e         = PurposeVersionUpdated(id.toString, pp)

      // when
      val conversion: Either[ComponentError, Option[NotificationPayload]] =
        PurposeEventsConverter.asNotificationPayload(e)
      // then
      conversion shouldBe Right(PurposePayload(id.toString, EventType.UPDATED.toString).some)
    }

    "Convert purpose version deleted to event payload" in {
      // given
      val id        = UUID.randomUUID()
      val versionId = UUID.randomUUID()
      val e         = PurposeVersionDeleted(id.toString, versionId.toString)

      // when
      val conversion: Either[ComponentError, Option[NotificationPayload]] =
        PurposeEventsConverter.asNotificationPayload(e)
      // then
      conversion shouldBe Right(PurposePayload(id.toString, EventType.DELETED.toString).some)
    }

    "Convert purpose deleted to event payload" in {
      // given
      val id = UUID.randomUUID()
      val e  = PurposeDeleted(id.toString)

      // when
      val conversion: Either[ComponentError, Option[NotificationPayload]] =
        PurposeEventsConverter.asNotificationPayload(e)
      // then
      conversion shouldBe Right(PurposePayload(id.toString, EventType.DELETED.toString).some)
    }

    "Convert purpose waited for approval to event payload" in {
      // given
      val id = UUID.randomUUID()
      val pp = getPurpose(id)
      val e  = PurposeVersionWaitedForApproval(pp)

      // when
      val conversion: Either[ComponentError, Option[NotificationPayload]] =
        PurposeEventsConverter.asNotificationPayload(e)
      // then
      conversion shouldBe Right(PurposePayload(id.toString, EventType.WAITING_FOR_APPROVAL.toString).some)
    }
  }

  private def getPurpose(id: UUID) = PersistentPurpose(
    id,
    eserviceId = UUID.randomUUID(),
    consumerId = UUID.randomUUID(),
    versions = Seq.empty,
    suspendedByConsumer = None,
    suspendedByProducer = None,
    title = "title",
    description = "description",
    riskAnalysisForm = None,
    createdAt = OffsetDateTime.now(),
    updatedAt = None,
    isFreeOfCharge = true,
    freeOfChargeReason = Some("BOH")
  )

  private def getPurposeVersion(versionId: UUID): PersistentPurposeVersion = PersistentPurposeVersion(
    id = versionId,
    state = Active,
    expectedApprovalDate = None,
    riskAnalysis = None,
    dailyCalls = 10,
    createdAt = OffsetDateTime.now(),
    updatedAt = None,
    firstActivationAt = None,
    suspendedAt = None
  )

}
