package it.pagopa.interop.notifier.model

import org.scanamo.DynamoFormat
import org.scanamo.generic.semiauto.deriveDynamoFormat

import java.util.UUID

final case class MessageId(resourceId: UUID, organizationId: UUID)

object MessageId {
  implicit val formatMessageId: DynamoFormat[MessageId] = deriveDynamoFormat
}
