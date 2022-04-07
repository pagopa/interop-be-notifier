package it.pagopa.interop.notifier.error

import it.pagopa.interop.commons.utils.errors.ComponentError

object NotifierErrors {

  final case class OrganizationNotFound(organizationId: String)
      extends ComponentError("0001", s"Organization $organizationId not found on registry")
  final case object OrganizationCreationFailed
      extends ComponentError("0002", "DB error while adding organization to registry")
  final case class OrganizationDeletionFailed(organizationId: String)
      extends ComponentError("0003", s"DB error while removing organization $organizationId from registry")
  final case class OrganizationUpdateFailed(organizationId: String)
      extends ComponentError("0004", s"DB error while updating organization $organizationId on registry")
  final case class OrganizationAlreadyExists(organizationId: String)
      extends ComponentError("0005", s"Organization $organizationId already existing")
}
