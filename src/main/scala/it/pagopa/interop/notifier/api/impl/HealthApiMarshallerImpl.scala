package it.pagopa.interop.notifier.api.impl

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import it.pagopa.interop.notifier.api.HealthApiMarshaller
import it.pagopa.interop.notifier.model.Problem
import spray.json.DefaultJsonProtocol

object HealthApiMarshallerImpl extends HealthApiMarshaller with SprayJsonSupport with DefaultJsonProtocol {

  override implicit def toEntityMarshallerProblem: ToEntityMarshaller[Problem] = entityMarshallerProblem
}
