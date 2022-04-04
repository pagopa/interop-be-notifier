package it.pagopa.interop.notifier.client

import scala.concurrent.ExecutionContext
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.services.sqs.SqsClientBuilder
import it.pagopa.interop.notifier.message.Message
import scala.concurrent.Future

import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import software.amazon.awssdk.services.sqs.model.SqsException
import java.util.UUID
import it.pagopa.interop.notifier.message.MessagePayload
import software.amazon.awssdk.services.sqs.model.SendMessageResponse
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry
import scala.jdk.CollectionConverters._
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse

final class QueueWriter(queueUrl: String, awsCredentials: AwsBasicCredentials)(implicit ec: ExecutionContext) {

  private val sqsClient: SqsClient =
    SqsClient
      .builder()
      .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
      .region(Region.EU_CENTRAL_1)
      .build()

  def send(message: Message): Future[SendMessageResponse] = Future {
    val sendMsgRequest: SendMessageRequest = SendMessageRequest
      .builder()
      .queueUrl(queueUrl)
      .messageBody(message.asJson)
      .messageGroupId(s"${message.eventJournalPersistenceId}_${message.eventJournalSequenceNumber}")
      .messageDeduplicationId(s"${message.eventJournalPersistenceId}_${message.eventJournalSequenceNumber}")
      .build()

    sqsClient.sendMessage(sendMsgRequest)
  }

  def sendBulk(messages: List[Message]): Future[SendMessageBatchResponse] = Future {

    def messageAdapter(m: Message): SendMessageBatchRequestEntry =
      SendMessageBatchRequestEntry
        .builder()
        // it is used to track the eventual failure for this specific message
        .id(UUID.randomUUID().toString())
        .messageBody(m.asJson)
        .messageGroupId(s"${m.eventJournalPersistenceId}_${m.eventJournalSequenceNumber}")
        .messageDeduplicationId(s"${m.eventJournalPersistenceId}_${m.eventJournalSequenceNumber}")
        .build()

    val sendMessageBatchRequest =
      SendMessageBatchRequest
        .builder()
        .queueUrl(queueUrl)
        .entries(messages.map(messageAdapter).asJavaCollection)
        .build()

    sqsClient.sendMessageBatch(sendMessageBatchRequest)
  }

}

object Foo extends App {
  val message =
    Message(UUID.randomUUID(), "ciao1", 1001L, System.currentTimeMillis(), MessagePayload("pippo", "pluto", Map.empty))

  val message2 =
    Message(UUID.randomUUID(), "ciao2", 1002L, System.currentTimeMillis(), MessagePayload("pape", "pluto", Map.empty))

  import ExecutionContext.Implicits.global

  val queueUrl       = ""
  val awsCredentials = AwsBasicCredentials.create("", "")
  val queue          = new QueueWriter(queueUrl, awsCredentials)

  val response = queue.sendBulk(message :: message2 :: Nil)

  Await.ready(response.map(println), Duration.Inf)
}
