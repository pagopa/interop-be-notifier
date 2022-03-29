package it.pagopa.interop.notifier.api.impl

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import it.pagopa.interop.notifier.api.RegistryApiMarshaller
import it.pagopa.interop.notifier.model.{Organization, OrganizationUpdatePayload, Problem}
import spray.json.DefaultJsonProtocol

object RegistryApiMarshallerImpl extends RegistryApiMarshaller with SprayJsonSupport with DefaultJsonProtocol {

  override implicit def toEntityMarshallerProblem: ToEntityMarshaller[Problem] = sprayJsonMarshaller[Problem]

  override implicit def fromEntityUnmarshallerOrganization: FromEntityUnmarshaller[Organization] =
    sprayJsonUnmarshaller[Organization]

  override implicit def fromEntityUnmarshallerOrganizationUpdatePayload
    : FromEntityUnmarshaller[OrganizationUpdatePayload] = sprayJsonUnmarshaller[OrganizationUpdatePayload]

  override implicit def toEntityMarshallerOrganization: ToEntityMarshaller[Organization] =
    sprayJsonMarshaller[Organization]
}
