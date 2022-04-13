package it.pagopa.interop.notifier.service.impl

import it.pagopa.interop.notifier.model.Message

import scala.concurrent.Future

class QueueReader() {

  def receiveN(n: Int): Future[List[Message]] = ???

  def handle[T](f: Message => Future[T]): Future[Unit] = {
    ???
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
