package it.pagopa.interop.notifier.model

import java.util.UUID

sealed trait DynamoQuery

final case class NotificationQuery(organizationId: UUID, eventId: Long) extends DynamoQuery

final case class IndexQuery(resourceId: UUID) extends DynamoQuery
