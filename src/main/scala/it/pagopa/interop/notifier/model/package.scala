package it.pagopa.interop.notifier

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

package object model extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val notificationFormat: RootJsonFormat[Notification] = jsonFormat4(Notification)

  implicit def toEntityNotificationMarshaller: ToEntityMarshaller[Notification] = sprayJsonMarshaller[Notification]
  implicit def fromEntityNotificationUnmarshaller: FromEntityUnmarshaller[Notification] =
    sprayJsonUnmarshaller[Notification]
}
