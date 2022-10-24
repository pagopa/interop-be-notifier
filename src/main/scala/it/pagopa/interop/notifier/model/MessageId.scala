package it.pagopa.interop.notifier.model

import org.scanamo.DynamoFormat
import org.scanamo.generic.semiauto.deriveDynamoFormat

final case class MessageId(resourceId: String, organizationId: String)

object MessageId {
  implicit val formatMessageId: DynamoFormat[MessageId] = deriveDynamoFormat
}
