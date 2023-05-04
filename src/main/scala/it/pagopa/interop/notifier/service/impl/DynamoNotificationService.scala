package it.pagopa.interop.notifier.service.impl

import cats.implicits._
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.notifier.common.system.ApplicationConfiguration
import it.pagopa.interop.notifier.error.NotifierErrors.DynamoReadingError
import it.pagopa.interop.notifier.model.NotificationMessage
import it.pagopa.interop.notifier.model.NotificationMessage._
import org.scanamo.DynamoReadError.describe
import org.scanamo._
import org.scanamo.ops.ScanamoOps
import org.scanamo.syntax._

import scala.concurrent.{ExecutionContext, Future}

class DynamoNotificationService(scanamo: ScanamoAsync) {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  val messages: Table[NotificationMessage] =
    Table[NotificationMessage](ApplicationConfiguration.dynamoNotificationTableName)

  def put(message: NotificationMessage): Future[Unit] = scanamo.exec(messages.put(message))

  def get(limit: Int)(organizationId: String, eventId: Long)(implicit
    ec: ExecutionContext,
    contexts: Seq[(String, String)]
  ): Future[List[NotificationMessage]] = {
    logger.debug(s"Getting $limit events for organization $organizationId from $eventId")
    val operations: ScanamoOps[List[Either[DynamoReadError, NotificationMessage]]] =
      messages.query("organizationId" === organizationId and "eventId" > eventId)
    scanamo.exec(operations).map(_.sequence).flatMap {
      case Right(x)  =>
        logger.debug(s"${x.size} messages retrieved from Dynamo")
        Future.successful(x.take(limit)) // TODO consider improving this using the limit upstream
      case Left(err) => Future.failed(DynamoReadingError(describe(err)))
    }
  }

}
