package it.pagopa.interop.notifier.model

import it.pagopa.interop.commons.queue.message.ProjectableEvent
import it.pagopa.interop.commons.utils.errors.ComponentError
import it.pagopa.interop.notifier.service.converters.{
  AgreementEventsConverter,
  CatalogEventsConverter,
  PurposeEventsConverter,
  notFoundPayload
}

object NotificationPayload {
  def create(event: ProjectableEvent): Either[ComponentError, Option[NotificationPayload]] = {
    val composed: PartialFunction[ProjectableEvent, Either[ComponentError, Option[NotificationPayload]]] =
      PurposeEventsConverter.asNotificationPayload orElse
        AgreementEventsConverter.asNotificationPayload orElse
        CatalogEventsConverter.asNotificationPayload

    composed.applyOrElse(event, notFoundPayload)

  }
}

sealed trait NotificationPayload {

  /**
    * Defines type of this object, e.g.: AGREEMENT, PURPOSE, etc.
    */
  val objectType: NotificationObjectType

  /**
    * Defines type of this event, e.g.: ADDED, UPDATED, DELETED etc.
    */
  val eventType: String

  /**
    * Defines the dataset of properties for uniquely identify the entity affected by the event
    */
  def objectId: Map[String, String]

}

final case class PurposePayload(
  purposeId: String,
  eventType: String,
  objectType: NotificationObjectType = NotificationObjectType.PURPOSE
) extends NotificationPayload {
  override val objectId: Map[String, String] = Map("purposeId" -> purposeId)
}

final case class AgreementPayload(
  agreementId: String,
  eventType: String,
  objectType: NotificationObjectType = NotificationObjectType.AGREEMENT
) extends NotificationPayload {
  override val objectId: Map[String, String] = Map("agreementId" -> agreementId)
}

final case class EServicePayload(
  eServiceId: String,
  descriptorId: Option[String],
  eventType: String,
  objectType: NotificationObjectType = NotificationObjectType.ESERVICE
) extends NotificationPayload {
  override val objectId: Map[String, String] =
    Map("eServiceId" -> eServiceId) ++ descriptorId.fold(Map.empty[String, String])(d => Map("descriptorId" -> d))
}
