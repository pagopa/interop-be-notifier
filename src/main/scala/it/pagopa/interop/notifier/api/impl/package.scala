package it.pagopa.interop.notifier.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.StatusCode
import it.pagopa.interop.commons.utils.errors.ComponentError
import it.pagopa.interop.notifier.model._
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

package object impl extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val problemErrorFormat: RootJsonFormat[ProblemError] = jsonFormat2(ProblemError)
  implicit val problemFormat: RootJsonFormat[Problem]           = jsonFormat5(Problem)
  implicit val eventFormat: RootJsonFormat[Event]               = jsonFormat4(Event)
  implicit val eventsFormat: RootJsonFormat[Events]             = jsonFormat2(Events)

  final val entityMarshallerProblem: ToEntityMarshaller[Problem] = sprayJsonMarshaller[Problem]

  final val serviceErrorCodePrefix: String = "017"
  final val defaultProblemType: String     = "about:blank"
  final val defaultErrorMessage: String    = "Unknown error"

  def problemOf(httpError: StatusCode, error: ComponentError): Problem =
    Problem(
      `type` = defaultProblemType,
      status = httpError.intValue,
      title = httpError.defaultMessage,
      errors = Seq(
        ProblemError(
          code = s"$serviceErrorCodePrefix-${error.code}",
          detail = Option(error.getMessage).getOrElse(defaultErrorMessage)
        )
      )
    )

  def problemOf(httpError: StatusCode, errors: List[ComponentError]): Problem =
    Problem(
      `type` = defaultProblemType,
      status = httpError.intValue,
      title = httpError.defaultMessage,
      errors = errors.map(error =>
        ProblemError(
          code = s"$serviceErrorCodePrefix-${error.code}",
          detail = Option(error.getMessage).getOrElse(defaultErrorMessage)
        )
      )
    )
}
