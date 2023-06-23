package it.pagopa.interop.notifier.model

import org.scanamo.DynamoFormat

sealed trait NotificationObjectType

object NotificationObjectType {

  case object AGREEMENT                    extends NotificationObjectType
  case object AGREEMENT_VERIFIED_ATTRIBUTE extends NotificationObjectType
  case object ESERVICE                     extends NotificationObjectType
  case object KEY                          extends NotificationObjectType
  case object PURPOSE                      extends NotificationObjectType

  implicit val notificationObjectTypeDynamoFormat: DynamoFormat[NotificationObjectType] =
    DynamoFormat.coercedXmap[NotificationObjectType, String, IllegalArgumentException](
      {
        case "AGREEMENT"                    => AGREEMENT
        case "AGREEMENT_VERIFIED_ATTRIBUTE" => AGREEMENT_VERIFIED_ATTRIBUTE
        case "ESERVICE"                     => ESERVICE
        case "KEY"                          => KEY
        case "PURPOSE"                      => PURPOSE
        case other => throw new IllegalArgumentException(s"$other is not a NotificationObjectType")
      },
      _.toString
    )

}
