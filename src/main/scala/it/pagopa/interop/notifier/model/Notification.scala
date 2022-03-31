package it.pagopa.interop.notifier.model

/**
  * Models the expected layout of interop notification messages
  * @param eventId - identifier of the notification event
  * @param eventType - type of the notified event
  * @param objectType - type of the object bound to this event
  * @param objectId - composite structure holding the identifier of the current object
  */
final case class Notification(eventId: String, eventType: String, objectType: String, objectId: Map[String, String])
