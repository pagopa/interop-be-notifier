package it.pagopa.interop.notifier.model

object Adapters {
  implicit class NotificationOps(val notificationObjectType: NotificationObjectType) extends AnyVal {
    def toApi: ObjectType = notificationObjectType match {
      case NotificationObjectType.AGREEMENT                    => ObjectType.AGREEMENT
      case NotificationObjectType.AGREEMENT_VERIFIED_ATTRIBUTE => ObjectType.AGREEMENT_VERIFIED_ATTRIBUTE
      case NotificationObjectType.ESERVICE                     => ObjectType.ESERVICE
      case NotificationObjectType.KEY                          => ObjectType.KEY
      case NotificationObjectType.PURPOSE                      => ObjectType.PURPOSE
    }
  }
}
