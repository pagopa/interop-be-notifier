package it.pagopa.interop.notifier.model

sealed trait DynamoEventPayload {

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

case class PurposeEventPayload(purposeId: String, eventType: String, objectType: String = "PURPOSE")
    extends DynamoEventPayload {
  override val objectId: Map[String, String] = Map("purposeId" -> purposeId)
}
case class AgreementEventPayload(agreementId: String, eventType: String, objectType: String = "AGREEMENT")
    extends DynamoEventPayload {
  override def objectId: Map[String, String] = Map("agreementId" -> agreementId)
}
