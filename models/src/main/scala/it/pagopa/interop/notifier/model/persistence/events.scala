package it.pagopa.interop.notifier.model.persistence

sealed trait Event                                              extends Persistable
final case class EventIdAdded(organizationId: String, id: Long) extends Event
