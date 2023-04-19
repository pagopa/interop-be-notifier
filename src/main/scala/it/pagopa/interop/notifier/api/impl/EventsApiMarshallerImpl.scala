package it.pagopa.interop.notifier.api.impl

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import it.pagopa.interop.notifier.api.EventsApiMarshaller
import it.pagopa.interop.notifier.model._
import spray.json.{RootJsonFormat, DefaultJsonProtocol}

object EventsApiMarshallerImpl extends EventsApiMarshaller with SprayJsonSupport with DefaultJsonProtocol {

  override implicit def toEntityMarshallerProblem: ToEntityMarshaller[Problem] = sprayJsonMarshaller[Problem]

  override implicit def toEntityMarshallerEvents: ToEntityMarshaller[Events] = sprayJsonMarshaller[Events]

  implicit val problemFormat: RootJsonFormat[Problem] = jsonFormat6(Problem)
  implicit val eventFormat: RootJsonFormat[Event]     = jsonFormat4(Event)
  implicit val eventsFormat: RootJsonFormat[Events]   = jsonFormat2(Events)
}
