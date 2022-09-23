package it.pagopa.interop.notifier.model

import it.pagopa.interop.commons.queue.message.Message

sealed trait DynamoRecord

final case class NotificationRecord(messageId: MessageId, eventId: Long, message: Message) extends DynamoRecord

final case class IndexRecord(messageId: MessageId) extends DynamoRecord
