package it.pagopa.interop.notifier.service.impl

import cats.implicits._
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.notifier.error.NotifierErrors.DynamoReadingError
import it.pagopa.interop.notifier.model.NotificationMessage._
import it.pagopa.interop.notifier.model.{NotificationMessage, NotificationQuery, NotificationRecord}
import it.pagopa.interop.notifier.service.DynamoService
import org.scanamo.DynamoReadError.describe
import org.scanamo._
import org.scanamo.ops.ScanamoOps
import org.scanamo.syntax._
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient

import scala.concurrent.{ExecutionContext, Future}

class DynamoNotificationService(tableName: String)(implicit ec: ExecutionContext)
    extends DynamoService[NotificationRecord, NotificationQuery, NotificationMessage] {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  private val dynamoClient: DynamoDbAsyncClient = DynamoDbAsyncClient.create()

  val scanamo: ScanamoAsync                = ScanamoAsync(dynamoClient)
  val messages: Table[NotificationMessage] = Table[NotificationMessage](tableName)

  override def put(message: NotificationRecord): Future[Unit] =
    for {
      msg    <- toNotificationMessage(message.messageId, message.eventId, message.message).toFuture
      result <- scanamo.exec(messages.put(msg))
    } yield result

  override def get(
    limit: Int
  )(query: NotificationQuery)(implicit contexts: Seq[(String, String)]): Future[List[NotificationMessage]] = {
    logger.debug(s"Getting $limit events for organization ${query.organizationId} from ${query.eventId}")
    val operations: ScanamoOps[List[Either[DynamoReadError, NotificationMessage]]] =
      messages.query("organizationId" === query.organizationId and "eventId" > query.eventId)
    scanamo.exec(operations).map(x => x.sequence).flatMap {
      case Right(x)  =>
        logger.debug(s"${x.size} messages retrieved from Dynamo")
        Future.successful(x.take(limit)) // TODO consider improving this using the limit upstream
      case Left(err) => Future.failed(DynamoReadingError(describe(err)))
    }
  }

  override def getOne(
    query: NotificationQuery
  )(implicit contexts: Seq[(String, String)]): Future[Option[NotificationMessage]] = {
    logger.debug(s"Getting organization using organizationId=${query.organizationId}/eventId=${query.eventId}")
    val operations: ScanamoOps[Option[Either[DynamoReadError, NotificationMessage]]] =
      messages.get("organizationId" === query.organizationId and "eventId" === query.organizationId)
    scanamo.exec(operations).map(_.sequence).flatMap {
      case Right(message) =>
        logger.debug(s"${message.size} message retrieved from Dynamo")
        Future.successful(message)
      case Left(err)      => Future.failed(DynamoReadingError(describe(err)))
    }
  }
}
