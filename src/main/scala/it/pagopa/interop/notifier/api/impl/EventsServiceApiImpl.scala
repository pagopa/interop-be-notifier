package it.pagopa.interop.notifier.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, onComplete}
import akka.http.scaladsl.server.{Route, StandardRoute}
import com.typesafe.scalalogging.Logger
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.AkkaUtils.getSubFuture
import it.pagopa.interop.commons.utils.TypeConversions.StringOps
import it.pagopa.interop.commons.utils.errors.GenericComponentErrors
import it.pagopa.interop.notifier.api.EventsApiService
import it.pagopa.interop.notifier.error.NotifierErrors.InternalServerError
import it.pagopa.interop.notifier.model.{Messages, Problem}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class EventsServiceApiImpl()(implicit ec: ExecutionContext) extends EventsApiService {

  private val logger = Logger.takingImplicit[ContextFieldsToLog](LoggerFactory.getLogger(this.getClass))

  /**
    * Code: 200, Message: Messages, DataType: Messages
    * Code: 400, Message: Bad request, DataType: Problem
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Events not found, DataType: Problem
    */
  override def getEventsFromId(lastEventId: String, limit: Int)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerMessages: ToEntityMarshaller[Messages]
  ): Route = {
    logger.info("Retrieving {} messages from id {}", limit, lastEventId)

    val result: Future[Messages] = for {
      _        <- getSubFuture(contexts).flatMap(_.toFutureUUID) // TODO get organizationId
      messages <- Future.successful(
        Messages(limit, size = 0, nextId = "", messages = List.empty)
      ) // TODO implement this
    } yield messages.toModel

    onComplete(result) {
      case Success(messages)                                         =>
        getEventsFromId200(messages)
      case Failure(ex: GenericComponentErrors.ResourceNotFoundError) =>
        logger.error(s"Error while retrieving messages, not found")
        val problem = problemOf(StatusCodes.NotFound, ex)
        getEventsFromId404(problem)
      case Failure(ex) => internalServerError(s"Error while getting agreement - ${ex.getMessage}")
    }
  }

  private[this] def internalServerError(message: String)(implicit c: ContextFieldsToLog): StandardRoute = {
    logger.error(message)
    complete(StatusCodes.InternalServerError, problemOf(StatusCodes.InternalServerError, InternalServerError))
  }
}
