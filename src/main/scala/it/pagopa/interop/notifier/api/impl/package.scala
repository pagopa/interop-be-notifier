package it.pagopa.interop.notifier.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCode
import it.pagopa.interop.commons.utils.errors.ComponentError
import it.pagopa.interop.notifier.model._
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

package object impl extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val problemErrorFormat: RootJsonFormat[ProblemError]   = jsonFormat2(ProblemError)
  implicit val problemFormat: RootJsonFormat[Problem]             = jsonFormat5(Problem)
  implicit val organizationFormat: RootJsonFormat[Message]        = jsonFormat4(Message)
  implicit val organizationUpdateFormat: RootJsonFormat[Messages] = jsonFormat4(Messages)

  // TODO implement this
  implicit class EnrichedMessages(private val response: Messages) extends AnyVal {
    def toModel: Messages = Messages(
      limit = response.limit,
      size = response.messages.size,
      nextId = response.nextId,
      messages = response.messages.map(messageToModel)
    )

    def messageToModel(msg: Message): Message =
      Message(eventId = msg.eventId, eventType = msg.eventType, objectType = msg.objectType, objectId = msg.objectId)
  }

  final val serviceErrorCodePrefix: String = "015"
  final val defaultProblemType: String     = "about:blank"

  def problemOf(httpError: StatusCode, error: ComponentError, defaultMessage: String = "Unknown error"): Problem =
    Problem(
      `type` = defaultProblemType,
      status = httpError.intValue,
      title = httpError.defaultMessage,
      errors = Seq(
        ProblemError(
          code = s"$serviceErrorCodePrefix-${error.code}",
          detail = Option(error.getMessage).getOrElse(defaultMessage)
        )
      )
    )
}
