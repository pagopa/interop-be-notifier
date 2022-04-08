package it.pagopa.interop.notifier.error

import it.pagopa.interop.commons.utils.errors.ComponentError

object NotifierErrors {

  final case object InternalServerError extends ComponentError("0001", "There was an internal server error")
  final case class OrganizationNotFound(organizationId: String)
      extends ComponentError("0002", s"Organization $organizationId not found")
}
