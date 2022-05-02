package it.pagopa.interop.notifier.service.converters

import it.pagopa.interop.commons.utils.errors.ComponentError
import it.pagopa.interop.notifier.model.{DynamoEventPayload, PurposeEventPayload}
import it.pagopa.interop.purposemanagement.model.persistence.{PurposeCreated, PurposeVersionWaitedForApproval}
import it.pagopa.interop.purposemanagement.model.purpose.PersistentPurpose
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.time.OffsetDateTime
import java.util.UUID

class PurposeEventsConverterSpec extends AnyWordSpecLike with Matchers {

  "Purpose conversions" should {

    "Convert purpose created to event payload" in {
      // given
      val id = UUID.randomUUID()
      val pp = getPurpose(id)
      val p  = PurposeCreated(pp)

      // when
      val conversion: Either[ComponentError, DynamoEventPayload] = PurposeEventsConverter.asDynamoPayload(p)
      // then
      conversion shouldBe Right(PurposeEventPayload(id.toString, EventType.CREATED.toString))
    }

    "Convert purpose waited for approval to event payload" in {
      // given
      val id = UUID.randomUUID()
      val pp = getPurpose(id)
      val p  = PurposeVersionWaitedForApproval(pp)

      // when
      val conversion: Either[ComponentError, DynamoEventPayload] = PurposeEventsConverter.asDynamoPayload(p)
      // then
      conversion shouldBe Right(PurposeEventPayload(id.toString, EventType.WAITING_FOR_APPROVAL.toString))
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
    updatedAt = None
  )

}
