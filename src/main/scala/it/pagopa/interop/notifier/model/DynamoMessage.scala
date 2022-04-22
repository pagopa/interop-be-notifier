package it.pagopa.interop.notifier.model

import it.pagopa.interop.commons.queue.message.{Message, ProjectableEvent}
import it.pagopa.interop.commons.utils.errors.ComponentError
import it.pagopa.interop.notifier.service.converters.{AgreementEventsConverter, PurposeEventsConverter}
import org.scanamo.DynamoFormat
import org.scanamo.generic.semiauto.deriveDynamoFormat
import it.pagopa.interop.notifier.service.converters.notFoundPayload

import java.util.UUID

/**
  * Models data to be saved on Dynamo
  * @param organizationId - identifier of the organization this entry belongs to [partition key]
  * @param eventId - unique identifier per organization [sort key]
  * @param messageUUID - unique identifier of the messages, as coming from the queue
  * @param eventJournalPersistenceId - unique identifier of the persistent event, as coming from the persistence layer
  * @param eventJournalSequenceNumber - sequence number on the journal, as coming from the persistence layer
  * @param eventTimestamp - timestamp of the persistence event on the journal, as coming from the persistence layer
  * @param payload - actual event payload, modeled according to the specific event type 
  */
final case class DynamoMessage(
  organizationId: String,
  eventId: Long,
  messageUUID: UUID,
  eventJournalPersistenceId: String,
  eventJournalSequenceNumber: Long,
  eventTimestamp: Long,
  payload: DynamoEventPayload
)

object DynamoMessage {
  def toDynamoMessage(organizationId: String, eventId: Long, message: Message): Either[ComponentError, DynamoMessage] =
    for {
      payload <- toDynamoPayload(message.payload)
    } yield DynamoMessage(
      organizationId,
      eventId,
      messageUUID = message.messageUUID,
      eventJournalPersistenceId = message.eventJournalPersistenceId,
      eventJournalSequenceNumber = message.eventJournalSequenceNumber,
      eventTimestamp = message.eventTimestamp,
      payload = payload
    )

  private[this] def toDynamoPayload(event: ProjectableEvent): Either[ComponentError, DynamoEventPayload] = {
    val composed =
      PurposeEventsConverter.asDynamoPayload orElse AgreementEventsConverter.asDynamoPayload orElse notFoundPayload
    composed(event)
  }
}

final object DynamoMessageFormatters {
  implicit val formatPayload: DynamoFormat[DynamoEventPayload]  = deriveDynamoFormat
  implicit val formatDynamoMessage: DynamoFormat[DynamoMessage] = deriveDynamoFormat
}
