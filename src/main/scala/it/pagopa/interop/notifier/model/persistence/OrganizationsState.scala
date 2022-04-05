package it.pagopa.interop.notifier.model.persistence

import it.pagopa.interop.notifier.model.persistence.organization.PersistentOrganization

final case class OrganizationsState(organizations: Map[String, PersistentOrganization]) extends Persistable {
  def deleteOrganization(organizationId: String): OrganizationsState =
    copy(organizations = organizations.removed(organizationId))

  def upsertOrganization(org: PersistentOrganization): OrganizationsState = {
    copy(organizations = organizations + (org.organizationId.toString -> org))
  }

}

object OrganizationsState {
  val empty: OrganizationsState = OrganizationsState(organizations = Map.empty[String, PersistentOrganization])
}
