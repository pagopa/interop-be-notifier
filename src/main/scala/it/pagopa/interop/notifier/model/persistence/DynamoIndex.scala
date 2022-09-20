package it.pagopa.interop.notifier.model.persistence

import java.util.UUID

final case class DynamoIndex(resourceId: UUID, organizationId: UUID)
