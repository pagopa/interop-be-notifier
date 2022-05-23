package it.pagopa.interop.notifier.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, onComplete}
import akka.http.scaladsl.server.{Route, StandardRoute}
import com.typesafe.scalalogging.Logger
import it.pagopa.interop.commons.jwt.{M2M_ROLE, authorizeInterop, hasPermissions}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.AkkaUtils.getClaimFuture
import it.pagopa.interop.commons.utils.ORGANIZATION_ID_CLAIM
import it.pagopa.interop.commons.utils.TypeConversions.StringOps
import it.pagopa.interop.commons.utils.errors.GenericComponentErrors
import it.pagopa.interop.commons.utils.errors.GenericComponentErrors.OperationForbidden
import it.pagopa.interop.notifier.api.EventsApiService
import it.pagopa.interop.notifier.error.NotifierErrors.InternalServerError
import it.pagopa.interop.notifier.model._
import it.pagopa.interop.notifier.service.DynamoService
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class EventsServiceApiImpl(dynamoService: DynamoService)(implicit ec: ExecutionContext) extends EventsApiService {

  private val logger = Logger.takingImplicit[ContextFieldsToLog](LoggerFactory.getLogger(this.getClass))

  private[this] def authorize(roles: String*)(
    route: => Route
  )(implicit contexts: Seq[(String, String)], toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
    authorizeInterop(
      hasPermissions(roles: _*),
      Problem(
        "error",
        status = 403,
        "Operation forbidden",
        Option(OperationForbidden.getMessage),
        Seq.empty[ProblemError]
      )
    ) {
      route
    }

  /**
    * Code: 200, Message: Messages, DataType: Messages
    * Code: 400, Message: Bad request, DataType: Problem
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Events not found, DataType: Problem
    */
  override def getEventsFromId(lastEventId: Long, limit: Int)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerEvents: ToEntityMarshaller[Events],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = authorize(M2M_ROLE) {
    logger.info(s"Retrieving $limit messages from id $lastEventId")

    val result: Future[Events] = for {
      organizationId <- getClaimFuture(contexts, ORGANIZATION_ID_CLAIM).flatMap(_.toFutureUUID)
      dynamoMessages <- dynamoService.get(limit)(organizationId, lastEventId)
      lastId   = Option.when(dynamoMessages.size > 0)(dynamoMessages.last.eventId)
      messages = Events(lastEventId = lastId, events = dynamoMessages.map(dynamoPayloadToEvent))
    } yield messages

    onComplete(result) {
      case Success(messages)                                         =>
        getEventsFromId200(messages)
      case Failure(ex: GenericComponentErrors.ResourceNotFoundError) =>
        logger.error(s"Error while retrieving events, not found")
        val problem = problemOf(StatusCodes.NotFound, ex)
        getEventsFromId404(problem)
      case Failure(ex) => internalServerError(s"Error while getting events - ${ex.getMessage}")
    }
  }

  private[this] def dynamoPayloadToEvent(message: DynamoMessage): Event =
    Event(
      eventId = message.eventId,
      eventType = message.payload.eventType,
      objectType = message.payload.objectType,
      objectId = message.payload.objectId
    )

  private[this] def internalServerError(message: String)(implicit c: ContextFieldsToLog): StandardRoute = {
    logger.error(message)
    complete(StatusCodes.InternalServerError, problemOf(StatusCodes.InternalServerError, InternalServerError))
  }

}
