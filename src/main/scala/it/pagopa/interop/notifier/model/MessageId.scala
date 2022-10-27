package it.pagopa.interop.notifier.model

import org.scanamo.DynamoFormat
import org.scanamo.generic.semiauto.deriveDynamoFormat

import java.util.UUID

/* organizationId is now a string because it also manages the value for all organizations which is not a UUID.
   More accurate management of this field is possible, but it could be an extra effort not necessary considering that
   this module will be substituted
 */
final case class MessageId(resourceId: UUID, organizationId: String)

object MessageId {
  implicit val formatMessageId: DynamoFormat[MessageId] = deriveDynamoFormat
}
