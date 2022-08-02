package it.pagopa.interop.notifier.model.persistence

final case class State(identifiers: Map[String, Long]) extends Persistable {
  def increaseEventId(organizationId: String, nextId: Long): State =
    copy(identifiers = identifiers + (organizationId -> nextId))
}

object State {
  val empty: State = State(identifiers = Map.empty[String, Long])
}
