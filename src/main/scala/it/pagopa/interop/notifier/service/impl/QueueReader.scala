package it.pagopa.interop.notifier.service.impl

import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import scala.concurrent.ExecutionContext
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import scala.concurrent.Future
import scala.jdk.CollectionConverters._
import software.amazon.awssdk.services.sqs.model.{Message => SQSMessage}
import it.pagopa.interop.notifier.message.Message
import cats.syntax.all._
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest

class QueueReader(queueUrl: String, awsCredentials: AwsBasicCredentials)(implicit ec: ExecutionContext) {

  private val sqsClient: SqsClient =
    SqsClient
      .builder()
      .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
      .region(Region.EU_CENTRAL_1)
      .build()

  private def rawReceiveN(n: Int): Future[List[SQSMessage]] = Future {
    val receiveMessageRequest: ReceiveMessageRequest = ReceiveMessageRequest
      .builder()
      .queueUrl(queueUrl)
      .maxNumberOfMessages(n)
      .build()
    sqsClient.receiveMessage(receiveMessageRequest).messages().asScala.toList
  }

  private def deleteMessage(handle: String): Future[Unit] = Future {
    val deleteMessageRequest: DeleteMessageRequest = DeleteMessageRequest
      .builder()
      .queueUrl(queueUrl)
      .receiptHandle(handle)
      .build()
    sqsClient.deleteMessage(deleteMessageRequest)
  }.void

  def receiveN(n: Int): Future[List[Message]] = for {
    rawMessages <- rawReceiveN(n)
    messages    <- Future.fromTry(rawMessages.map(_.body()).traverse(Message.from).toTry)
  } yield messages

  def handleN[T](n: Int)(f: Message => Future[T]): Future[List[T]] = for {
    rawMessages        <- rawReceiveN(n)
    messagesAndHandles <- rawMessages.traverse(message =>
      Future.fromTry(Message.from(message.body()).toTry).map((_, message.receiptHandle()))
    )
    result             <- messagesAndHandles
      .traverseFilter { case (message, handle) =>
        (f(message) <* deleteMessage(handle)).map(_.some).recover { case _ => None }
      }
  } yield result

  def handle[T](f: Message => Future[T]): Future[Unit] = {
    // Submitting to an ExecutionContext introduces an async boundary that reset the stack,
    // that makes Future behave like it's trampolining, so this function is stack safe.
    def loop: Future[List[T]] = handleN[T](10)(f).flatMap(_ => loop).recoverWith(_ => loop)
    loop.void
  }

}

/*object Foo extends App {
  import java.util.UUID
  import it.pagopa.interop.notifier.message.{Message, MessagePayload}
  import scala.concurrent.Await
  import scala.concurrent.duration.Duration

  val message =
    Message(UUID.randomUUID(), "ciao1", 1001L, System.currentTimeMillis(), MessagePayload("pippo", "pluto", Map.empty))

  val message2 =
    Message(UUID.randomUUID(), "ciao2", 1002L, System.currentTimeMillis(), MessagePayload("pape", "pluto", Map.empty))

  import ExecutionContext.Implicits.global

  val queueUrl       = ""
  val awsCredentials = AwsBasicCredentials.create("", "")
  val queue          = new QueueReader(queueUrl, awsCredentials)

  val response = queue.handleN(10)(m => Future(println(m.payload.eventype)))

  Await.ready(response.map(println), Duration.Inf)
}*/
