package it.pagopa.interop.notifier.api.impl

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import it.pagopa.interop.notifier.api.EventsApiMarshaller
import it.pagopa.interop.notifier.model.{Events, Problem}
import spray.json.DefaultJsonProtocol

object EventsApiMarshallerImpl extends EventsApiMarshaller with SprayJsonSupport with DefaultJsonProtocol {

  override implicit def toEntityMarshallerProblem: ToEntityMarshaller[Problem] = entityMarshallerProblem

  override implicit def toEntityMarshallerEvents: ToEntityMarshaller[Events] =
    sprayJsonMarshaller[Events]

}
