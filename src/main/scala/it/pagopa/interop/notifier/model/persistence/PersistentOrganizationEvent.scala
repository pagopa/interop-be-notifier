package it.pagopa.interop.notifier.model.persistence

import java.util.UUID

case class PersistentOrganizationEvent(organizationId: UUID, eventId: Long)
