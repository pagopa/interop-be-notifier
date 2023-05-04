package it.pagopa.interop.notifier.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import it.pagopa.interop.notifier.model._
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

package object impl extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val problemErrorFormat: RootJsonFormat[ProblemError] = jsonFormat2(ProblemError)
  implicit val problemFormat: RootJsonFormat[Problem]           = jsonFormat6(Problem)
  implicit val eventFormat: RootJsonFormat[Event]               = jsonFormat4(Event)
  implicit val eventsFormat: RootJsonFormat[Events]             = jsonFormat2(Events)

}
