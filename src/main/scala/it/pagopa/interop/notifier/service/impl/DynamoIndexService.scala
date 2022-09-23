package it.pagopa.interop.notifier.service.impl

import cats.implicits._
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.notifier.error.NotifierErrors.DynamoReadingError
import it.pagopa.interop.notifier.model.MessageId.formatMessageId
import it.pagopa.interop.notifier.model.{IndexQuery, IndexRecord, MessageId}
import it.pagopa.interop.notifier.service.DynamoService
import org.scanamo.DynamoReadError.describe
import org.scanamo._
import org.scanamo.ops.ScanamoOps
import org.scanamo.syntax._
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient

import scala.concurrent.{ExecutionContext, Future}

class DynamoIndexService(tableName: String)(implicit ec: ExecutionContext)
    extends DynamoService[IndexRecord, IndexQuery, MessageId] {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  private val dynamoClient: DynamoDbAsyncClient = DynamoDbAsyncClient.create()

  val scanamo: ScanamoAsync        = ScanamoAsync(dynamoClient)
  val messageIds: Table[MessageId] = Table[MessageId](tableName)

  override def put(message: IndexRecord): Future[Unit] = scanamo.exec(messageIds.put(message.messageId))

  override def get(limit: Int)(query: IndexQuery)(implicit contexts: Seq[(String, String)]): Future[List[MessageId]] = {
    logger.debug(s"Getting $limit events for resourceId ${query.resourceId}")
    val operations = messageIds.query("resourceId" === query.resourceId)
    scanamo.exec(operations).map(x => x.sequence).flatMap {
      case Right(x)  =>
        logger.debug(s"${x.size} messages retrieved from Dynamo")
        Future.successful(x.take(limit)) // TODO consider improving this using the limit upstream
      case Left(err) => Future.failed(DynamoReadingError(describe(err)))
    }
  }

  override def getOne(query: IndexQuery)(implicit contexts: Seq[(String, String)]): Future[Option[MessageId]] = {
    logger.debug(s"Getting messageId using resourceId=${query.resourceId}")
    val operations: ScanamoOps[Option[Either[DynamoReadError, MessageId]]] =
      messageIds.get("resourceId" === query.resourceId)
    scanamo.exec(operations).map(_.sequence).flatMap {
      case Right(messageId) =>
        logger.debug(s"${messageId.size} message retrieved from Dynamo")
        Future.successful(messageId)
      case Left(err)        => Future.failed(DynamoReadingError(describe(err)))
    }
  }
}
