package it.pagopa.interop.notifier.model

import it.pagopa.interop.commons.queue.message.Message
import it.pagopa.interop.commons.utils.errors.ComponentError
import org.scanamo.DynamoFormat
import org.scanamo.generic.semiauto.deriveDynamoFormat

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
final case class NotificationMessage(
  organizationId: String,
  eventId: Long,
  messageUUID: UUID,
  eventJournalPersistenceId: String,
  eventJournalSequenceNumber: Long,
  eventTimestamp: Long,
  resourceId: String,
  payload: NotificationPayload
)

object NotificationMessage {
  implicit val formatNotificationObjectType: DynamoFormat[NotificationObjectType] = deriveDynamoFormat
  implicit val formatNotificationPayload: DynamoFormat[NotificationPayload]       = deriveDynamoFormat
  implicit val formatNotificationMessage: DynamoFormat[NotificationMessage]       = deriveDynamoFormat
  def create(messageId: MessageId, eventId: Long, message: Message): Either[ComponentError, NotificationMessage] =
    NotificationPayload
      .create(message.payload)
      .map(payload =>
        NotificationMessage(
          organizationId = messageId.organizationId,
          eventId = eventId,
          messageUUID = message.messageUUID,
          eventJournalPersistenceId = message.eventJournalPersistenceId,
          eventJournalSequenceNumber = message.eventJournalSequenceNumber,
          eventTimestamp = message.eventTimestamp,
          resourceId = messageId.resourceId.toString,
          payload = payload
        )
      )
}
