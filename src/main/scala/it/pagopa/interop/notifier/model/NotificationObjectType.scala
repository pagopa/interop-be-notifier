package it.pagopa.interop.notifier.model

sealed trait NotificationObjectType

object NotificationObjectType {

  case object AGREEMENT                    extends NotificationObjectType
  case object AGREEMENT_VERIFIED_ATTRIBUTE extends NotificationObjectType
  case object ESERVICE                     extends NotificationObjectType
  case object KEY                          extends NotificationObjectType
  case object PURPOSE                      extends NotificationObjectType

}
