package it.pagopa.interop.notifier.error

import it.pagopa.interop.commons.utils.errors.ComponentError

object NotifierErrors {

  final case object InternalServerError extends ComponentError("0001", "There was an internal server error")
  final case class OrganizationNotFound(organizationId: String)
      extends ComponentError("0002", s"Organization $organizationId not found")

  final case class DynamoReadingError(message: String)
      extends ComponentError("0003", s"Error while reading data from Dynamo -> $message")
  final case class MessageRecipientNotFound(message: String)
      extends ComponentError("0004", s"Error while parsing message content -> $message")

  final case class DynamoConverterNotFound(message: String)
      extends ComponentError("0005", s"Error while converting message to Dynamo payload -> $message")

}
