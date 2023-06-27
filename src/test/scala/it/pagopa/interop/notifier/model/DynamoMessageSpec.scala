package it.pagopa.interop.notifier.model

import it.pagopa.interop.agreementmanagement.model.agreement.{Active, PersistentAgreement, PersistentStamps}
import it.pagopa.interop.agreementmanagement.model.persistence.AgreementActivated
import it.pagopa.interop.commons.queue.message.Message
import it.pagopa.interop.commons.utils.errors.ComponentError
import it.pagopa.interop.notifier.service.converters.EventType.{CREATED, UPDATED}
import it.pagopa.interop.purposemanagement.model.persistence.PurposeCreated
import it.pagopa.interop.purposemanagement.model.purpose.PersistentPurpose
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.time.OffsetDateTime
import java.util.UUID
import cats.syntax.all._

class DynamoMessageSpec extends AnyWordSpecLike with Matchers {

  "Dynamo messages conversions" should {
    "Convert purpose created message to dynamo message" in {
      // given
      val id                         = UUID.randomUUID()
      val messageId                  = UUID.randomUUID()
      val eventJournalPersistenceId  = "id"
      val eventJournalSequenceNumber = 1L
      val eventTimestamp             = 1L
      val kind                       = "KIND"

      val pp    = PersistentPurpose(
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
      val event = PurposeCreated(pp)

      val message =
        Message(messageId, eventJournalPersistenceId, eventJournalSequenceNumber, eventTimestamp, kind, payload = event)

      val organizationId = UUID.randomUUID().toString()
      val eventId        = 1L

      // when
      val conversion: Either[ComponentError, Option[NotificationMessage]] =
        NotificationMessage.create(MessageId(resourceId = id, organizationId = organizationId), eventId, message)
      // then
      val expected                                                        = NotificationMessage(
        organizationId,
        eventId,
        messageUUID = messageId,
        eventJournalPersistenceId = eventJournalPersistenceId,
        eventJournalSequenceNumber = eventJournalSequenceNumber,
        eventTimestamp = eventTimestamp,
        payload = PurposePayload(id.toString, CREATED.toString),
        resourceId = id.toString
      )
      conversion shouldBe Right(expected.some)
    }

    "Convert agreement created message to dynamo message" in {
      // given
      val id                         = UUID.randomUUID()
      val messageId                  = UUID.randomUUID()
      val eventJournalPersistenceId  = "id"
      val eventJournalSequenceNumber = 1L
      val eventTimestamp             = 1L
      val kind                       = "KIND"

      val pa = PersistentAgreement(
        id,
        eserviceId = UUID.randomUUID(),
        descriptorId = UUID.randomUUID(),
        producerId = UUID.randomUUID(),
        consumerId = UUID.randomUUID(),
        state = Active,
        verifiedAttributes = Seq.empty,
        certifiedAttributes = Seq.empty,
        declaredAttributes = Seq.empty,
        suspendedByConsumer = None,
        suspendedByProducer = None,
        suspendedByPlatform = None,
        consumerDocuments = List.empty,
        createdAt = OffsetDateTime.now(),
        suspendedAt = None,
        updatedAt = None,
        consumerNotes = None,
        contract = None,
        stamps = PersistentStamps(),
        rejectionReason = None
      )

      val event = AgreementActivated(pa)

      val message =
        Message(messageId, eventJournalPersistenceId, eventJournalSequenceNumber, eventTimestamp, kind, payload = event)

      val organizationId = UUID.randomUUID().toString()
      val eventId        = 1L

      // when
      val conversion: Either[ComponentError, Option[NotificationMessage]] =
        NotificationMessage.create(MessageId(resourceId = id, organizationId = organizationId), eventId, message)
      // then
      val expected                                                        = NotificationMessage(
        organizationId,
        eventId,
        messageUUID = messageId,
        eventJournalPersistenceId = eventJournalPersistenceId,
        eventJournalSequenceNumber = eventJournalSequenceNumber,
        eventTimestamp = eventTimestamp,
        payload = AgreementPayload(id.toString, UPDATED.toString),
        resourceId = id.toString
      )
      conversion shouldBe Right(expected.some)
    }

  }

}
