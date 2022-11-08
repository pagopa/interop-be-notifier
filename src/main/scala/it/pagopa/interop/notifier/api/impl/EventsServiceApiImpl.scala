package it.pagopa.interop.notifier.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, onComplete}
import akka.http.scaladsl.server.{Route, StandardRoute}
import com.typesafe.scalalogging.Logger
import it.pagopa.interop.commons.jwt.{M2M_ROLE, authorizeInterop, hasPermissions}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.AkkaUtils.getOrganizationIdFutureUUID
import it.pagopa.interop.commons.utils.errors.GenericComponentErrors
import it.pagopa.interop.commons.utils.errors.GenericComponentErrors.OperationForbidden
import it.pagopa.interop.notifier.api.EventsApiService
import it.pagopa.interop.notifier.error.NotifierErrors.InternalServerError
import it.pagopa.interop.notifier.model._
import it.pagopa.interop.notifier.service.converters.allOrganizations
import it.pagopa.interop.notifier.service.impl.DynamoNotificationService
import org.scanamo.ScanamoAsync

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

final class EventsServiceApiImpl(dynamoNotificationService: DynamoNotificationService)(implicit
  scanamo: ScanamoAsync,
  ec: ExecutionContext
) extends EventsApiService {

  private val logger = Logger.takingImplicit[ContextFieldsToLog](this.getClass())

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

  override def getEventsFromId(lastEventId: Long, limit: Int, fromAllOrganizations: Boolean)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerEvents: ToEntityMarshaller[Events],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = authorize(M2M_ROLE) {
    logger.info(s"Retrieving $limit messages from id $lastEventId")

    val result: Future[Events] = for {
      organizationId <- getOrganizationIdFutureUUID(contexts)
      dynamoMessages <-
        if (fromAllOrganizations) dynamoNotificationService.get(limit)(organizationId.toString, lastEventId)
        else dynamoNotificationService.get(limit)(allOrganizations, lastEventId)
      lastId   = Option.when(dynamoMessages.nonEmpty)(dynamoMessages.last.eventId)
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

  private[this] def dynamoPayloadToEvent(message: NotificationMessage): Event =
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
