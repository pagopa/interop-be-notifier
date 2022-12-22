package it.pagopa.interop.notifier.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Directives.onComplete
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.jwt._
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.AkkaUtils.getOrganizationIdFutureUUID
import it.pagopa.interop.notifier.api.EventsApiService
import it.pagopa.interop.notifier.api.impl.ResponseHandlers._
import it.pagopa.interop.notifier.model._
import it.pagopa.interop.notifier.service.converters.allOrganizations
import it.pagopa.interop.notifier.service.impl.DynamoNotificationService
import org.scanamo.ScanamoAsync

import scala.concurrent.{ExecutionContext, Future}

final class EventsServiceApiImpl(dynamoNotificationService: DynamoNotificationService)(implicit
  scanamo: ScanamoAsync,
  ec: ExecutionContext
) extends EventsApiService {

  private implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass())

  override def getEventsFromId(lastEventId: Long, limit: Int)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerEvents: ToEntityMarshaller[Events],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = authorize(M2M_ROLE) {
    val operationLabel = s"Retrieving $limit messages from id $lastEventId"
    logger.info(operationLabel)

    val result: Future[Events] = for {
      organizationId <- getOrganizationIdFutureUUID(contexts)
      events         <- getEvents(organizationId.toString, limit, lastEventId)
    } yield events

    onComplete(result) {
      getEventsFromIdResponse[Events](operationLabel)(getEventsFromId200)
    }
  }

  override def getAllEventsFromId(lastEventId: Long, limit: Int)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerEvents: ToEntityMarshaller[Events],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = authorize(M2M_ROLE) {
    val operationLabel = s"Retrieving all organizations events $limit messages from id $lastEventId"
    logger.info(operationLabel)

    val result: Future[Events] = getEvents(allOrganizations, limit, lastEventId)

    onComplete(result) {
      getAllEventsFromIdResponse[Events](operationLabel)(getEventsFromId200)
    }
  }

  private def getEvents(organizationId: String, limit: Int, lastEventId: Long)(implicit
    context: Seq[(String, String)]
  ): Future[Events] = for {
    dynamoMessages <- dynamoNotificationService.get(limit)(organizationId, lastEventId)
    lastId = dynamoMessages.lastOption.map(_.eventId)
    events = Events(lastEventId = lastId, events = dynamoMessages.map(dynamoPayloadToEvent))
  } yield events

  private[this] def dynamoPayloadToEvent(message: NotificationMessage): Event =
    Event(
      eventId = message.eventId,
      eventType = message.payload.eventType,
      objectType = message.payload.objectType,
      objectId = message.payload.objectId
    )

}
