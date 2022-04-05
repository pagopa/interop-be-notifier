package it.pagopa.interop.notifier.model.persistence

import it.pagopa.interop.notifier.model.persistence.organization.PersistentOrganization

sealed trait Event                           extends Persistable
sealed trait OrganizationEvent               extends Event
sealed trait OrganizationNotificationIdEvent extends Event

final case class OrganizationAdded(organization: PersistentOrganization)   extends OrganizationEvent
final case class OrganizationUpdated(organization: PersistentOrganization) extends OrganizationEvent
final case class OrganizationDeleted(organizationId: String)               extends OrganizationEvent

final case class EventIdAdded(organizationId: String, id: Long) extends OrganizationNotificationIdEvent
