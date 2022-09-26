package it.pagopa.interop.notifier.model.persistence

import java.util.UUID

final case class PersistentOrganizationEvent(organizationId: UUID, eventId: Long)
