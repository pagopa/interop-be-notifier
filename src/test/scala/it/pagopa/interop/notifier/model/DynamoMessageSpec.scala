package it.pagopa.interop.notifier.model

import it.pagopa.interop.agreementmanagement.model.agreement.{Active, PersistentAgreement}
import it.pagopa.interop.agreementmanagement.model.persistence.AgreementActivated
import it.pagopa.interop.commons.queue.message.Message
import it.pagopa.interop.commons.utils.errors.ComponentError
import it.pagopa.interop.notifier.service.converters.EventType.{ACTIVATED, CREATED}
import it.pagopa.interop.purposemanagement.model.persistence.PurposeCreated
import it.pagopa.interop.purposemanagement.model.purpose.PersistentPurpose
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.time.OffsetDateTime
import java.util.UUID

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
        updatedAt = None
      )
      val event = PurposeCreated(pp)

      val message =
        Message(messageId, eventJournalPersistenceId, eventJournalSequenceNumber, eventTimestamp, kind, payload = event)

      val organizationId = UUID.randomUUID().toString
      val eventId        = 1L

      // when
      val conversion: Either[ComponentError, DynamoMessage] =
        DynamoMessage.toDynamoMessage(organizationId, eventId, message)
      // then
      val expected                                          = DynamoMessage(
        organizationId,
        eventId,
        messageUUID = messageId,
        eventJournalPersistenceId = eventJournalPersistenceId,
        eventJournalSequenceNumber = eventJournalSequenceNumber,
        eventTimestamp = eventTimestamp,
        payload = PurposeEventPayload(id.toString, CREATED.toString)
      )
      conversion shouldBe Right(expected)
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
        suspendedByConsumer = None,
        suspendedByProducer = None,
        createdAt = OffsetDateTime.now(),
        updatedAt = None
      )

      val event = AgreementActivated(pa)

      val message =
        Message(messageId, eventJournalPersistenceId, eventJournalSequenceNumber, eventTimestamp, kind, payload = event)

      val organizationId = UUID.randomUUID().toString
      val eventId        = 1L

      // when
      val conversion: Either[ComponentError, DynamoMessage] =
        DynamoMessage.toDynamoMessage(organizationId, eventId, message)
      // then
      val expected                                          = DynamoMessage(
        organizationId,
        eventId,
        messageUUID = messageId,
        eventJournalPersistenceId = eventJournalPersistenceId,
        eventJournalSequenceNumber = eventJournalSequenceNumber,
        eventTimestamp = eventTimestamp,
        payload = AgreementEventPayload(id.toString, ACTIVATED.toString)
      )
      conversion shouldBe Right(expected)
    }

  }

}
