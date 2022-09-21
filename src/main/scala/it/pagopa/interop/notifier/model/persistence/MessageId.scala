package it.pagopa.interop.notifier.model.persistence

import java.util.UUID

final case class MessageId(resourceId: UUID, organizationId: UUID)
