package it.pagopa.interop.notifier.service.impl

import cats.implicits._
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.queue.message.Message
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.notifier.error.NotifierErrors.DynamoReadingError
import it.pagopa.interop.notifier.model.DynamoMessage
import it.pagopa.interop.notifier.model.DynamoMessage.toDynamoMessage
import it.pagopa.interop.notifier.model.DynamoMessageFormatters.formatDynamoMessage
import it.pagopa.interop.notifier.model.persistence.MessageId
import it.pagopa.interop.notifier.service.DynamoService
import org.scanamo.DynamoReadError.describe
import org.scanamo._
import org.scanamo.syntax._
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class DynamoServiceImpl(tableName: String, indexName: String)(implicit ec: ExecutionContext) extends DynamoService {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  private val dynamoClient: DynamoDbAsyncClient = DynamoDbAsyncClient.create()

  val scanamo: ScanamoAsync                        = ScanamoAsync(dynamoClient)
  val messages: Table[DynamoMessage]               = Table[DynamoMessage](tableName)
  val invertedIndex: SecondaryIndex[DynamoMessage] = messages.index(indexName)

  override def put(messageId: MessageId, eventId: Long, message: Message): Future[Unit] =
    for {
      msg    <- toDynamoMessage(messageId, eventId, message).toFuture
      result <- scanamo.exec(messages.put(msg))
    } yield result

  override def get(
    limit: Int
  )(organizationId: UUID, eventId: Long)(implicit contexts: Seq[(String, String)]): Future[List[DynamoMessage]] = {
    logger.debug(s"Getting $limit events for organization $organizationId from $eventId")
    val operations = messages.query("organizationId" === organizationId and "eventId" > eventId)
    scanamo.exec(operations).map(x => x.sequence).flatMap {
      case Right(x)  =>
        logger.debug(s"${x.size} messages retrieved from Dynamo")
        Future.successful(x.take(limit)) // TODO consider improving this using the limit upstream
      case Left(err) => Future.failed(DynamoReadingError(describe(err)))
    }
  }

  override def getOrganizationId(resourceId: UUID)(implicit contexts: Seq[(String, String)]): Future[Option[UUID]] = {
    logger.debug(s"Getting organization using $resourceId")
    val operations = invertedIndex.query("resourceId" === resourceId)
    scanamo.exec(operations).map(_.sequence).flatMap {
      case Right(messages) =>
        logger.debug(s"${messages.size} messages retrieved from Dynamo")
        val organizationId: Option[String] = messages.map(_.organizationId).distinct.headOption
        organizationId.traverse(_.toFutureUUID)
      case Left(err)       => Future.failed(DynamoReadingError(describe(err)))
    }
  }
}
