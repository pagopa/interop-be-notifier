package it.pagopa.interop.notifier.api.impl

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import it.pagopa.interop.notifier.api.EventsApiMarshaller
import it.pagopa.interop.notifier.model.{Messages, Problem}
import spray.json.DefaultJsonProtocol

object EventsApiMarshallerImpl extends EventsApiMarshaller with SprayJsonSupport with DefaultJsonProtocol {

  override implicit def toEntityMarshallerProblem: ToEntityMarshaller[Problem] = sprayJsonMarshaller[Problem]

  override implicit def toEntityMarshallerMessages: ToEntityMarshaller[Messages] =
    sprayJsonMarshaller[Messages]

}
