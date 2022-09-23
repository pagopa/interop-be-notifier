package it.pagopa.interop.notifier.model

sealed trait NotificationPayload {

  /**
    * Defines type of this object, e.g.: AGREEMENT, PURPOSE, etc.
    */
  val objectType: String

  /**
    * Defines type of this event, e.g.: ADDED, UPDATED, DELETED etc.
    */
  val eventType: String

  /**
    * Defines the dataset of properties for uniquely identify the entity affected by the event
    */
  def objectId: Map[String, String]

}

final case class PurposePayload(purposeId: String, eventType: String, objectType: String = "PURPOSE")
    extends NotificationPayload {
  override val objectId: Map[String, String] = Map("purposeId" -> purposeId)
}
final case class AgreementPayload(agreementId: String, eventType: String, objectType: String = "AGREEMENT")
    extends NotificationPayload {
  override val objectId: Map[String, String] = Map("agreementId" -> agreementId)
}
