package it.pagopa.interop.notifier.service.impl

import cats.free.Free
import cats.implicits._
import it.pagopa.interop.commons.queue.QueueAccountInfo
import it.pagopa.interop.commons.queue.message.Message
import it.pagopa.interop.commons.utils.TypeConversions.EitherOps
import it.pagopa.interop.notifier.error.NotifierErrors.DynamoReadingError
import it.pagopa.interop.notifier.model.DynamoMessage
import it.pagopa.interop.notifier.model.DynamoMessage.toDynamoMessage
import it.pagopa.interop.notifier.model.DynamoMessageFormatters.formatDynamoMessage
import it.pagopa.interop.notifier.service.DynamoService
import org.scanamo.DynamoReadError.describe
import org.scanamo._
import org.scanamo.ops.ScanamoOpsA
import org.scanamo.syntax._
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}

class DynamoServiceImpl(val config: QueueAccountInfo, val tableName: String)(implicit ec: ExecutionContext)
    extends DynamoService {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  private val awsCredentials: AwsBasicCredentials =
    AwsBasicCredentials.create(config.accessKeyId, config.secretAccessKey)

  private val dynamoClient: DynamoDbAsyncClient = DynamoDbAsyncClient
    .builder()
    .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
    .region(config.region)
    .build()

  val scanamo  = ScanamoAsync(dynamoClient)
  val messages = Table[DynamoMessage](tableName)

  override def put(organizationId: UUID, eventId: Long, message: Message): Future[Unit] = {
    def operation(msg: DynamoMessage): Free[ScanamoOpsA, Unit] = for {
      _ <- messages.put(msg)
    } yield ()

    for {
      msg <- toDynamoMessage(organizationId.toString, eventId, message).toFuture
      _   <- scanamo.exec { operation(msg) }
    } yield ()
  }

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
}
