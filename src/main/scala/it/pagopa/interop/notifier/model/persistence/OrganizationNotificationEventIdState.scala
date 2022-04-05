package it.pagopa.interop.notifier.model.persistence

final case class OrganizationNotificationEventIdState(identifiers: Map[String, Long]) extends Persistable {
  def increaseEventId(organizationId: String, nextId: Long): OrganizationNotificationEventIdState = {
    copy(identifiers = identifiers + (organizationId -> nextId))
  }

}

object OrganizationNotificationEventIdState {
  val empty: OrganizationNotificationEventIdState =
    OrganizationNotificationEventIdState(identifiers = Map.empty[String, Long])
}
