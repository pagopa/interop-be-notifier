package it.pagopa.interop.notifier.service.impl

import cats.implicits._
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.notifier.common.system.ApplicationConfiguration
import it.pagopa.interop.notifier.error.NotifierErrors.DynamoReadingError
import it.pagopa.interop.notifier.model.MessageId
import it.pagopa.interop.notifier.model.MessageId.formatMessageId
import org.scanamo.DynamoReadError.describe
import org.scanamo._
import org.scanamo.ops.ScanamoOps
import org.scanamo.syntax._

import scala.concurrent.{ExecutionContext, Future}

class DynamoNotificationResourcesService(scanamo: ScanamoAsync) {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  val messageIds: Table[MessageId] = Table[MessageId](ApplicationConfiguration.dynamoNotificationResourcesTableName)

  def put(messageId: MessageId): Future[Unit] = scanamo.exec(messageIds.put(messageId))

  def getOne(
    resourceId: String
  )(implicit ec: ExecutionContext, contexts: Seq[(String, String)]): Future[Option[MessageId]] = {
    logger.debug(s"Getting messageId using resourceId=$resourceId")
    val operations: ScanamoOps[Option[Either[DynamoReadError, MessageId]]] =
      messageIds.get("resourceId" === resourceId)
    scanamo.exec(operations).map(_.sequence).flatMap {
      case Right(messageId) =>
        logger.debug(s"${messageId.size} message retrieved from Dynamo")
        Future.successful(messageId)
      case Left(err)        => Future.failed(DynamoReadingError(describe(err)))
    }
  }
}
