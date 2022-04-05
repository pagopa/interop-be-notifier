package it.pagopa.interop.notifier.model.persistence.organization

import it.pagopa.interop.notifier.model.persistence.Persistent

import java.util.UUID

final case class PersistentOrganizationEvent(organizationId: UUID, eventId: Long) extends Persistent
