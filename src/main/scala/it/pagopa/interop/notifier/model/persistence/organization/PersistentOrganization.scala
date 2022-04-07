package it.pagopa.interop.notifier.model.persistence.organization

import it.pagopa.interop.notifier.model.Organization
import it.pagopa.interop.notifier.model.persistence.Persistent

import java.util.UUID

final case class PersistentOrganization(organizationId: UUID, notificationURL: String, audience: String)
    extends Persistent

object PersistentOrganization {
  def toPersistentOrganization(org: Organization): PersistentOrganization =
    PersistentOrganization(
      organizationId = org.organizationId,
      notificationURL = org.notificationURL,
      audience = org.audience
    )

  def toAPI(org: PersistentOrganization): Organization =
    Organization(organizationId = org.organizationId, notificationURL = org.notificationURL, audience = org.audience)
}
