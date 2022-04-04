package it.pagopa.interop.notifier.message

import java.util.UUID

import spray.json._
import spray.json.DefaultJsonProtocol._
import scala.util.Try
import Message._

final case class Message(
  messageUUID: UUID,
  eventJournalPersistenceId: String,
  eventJournalSequenceNumber: Long,
  eventTimestamp: Long,
  payload: MessagePayload
) {
  def asJson: String = this.toJson.compactPrint
}

final case class MessagePayload(eventype: String, objectType: String, objectId: Map[String, String] = Map.empty)

object Message {
  implicit val payloadFormat: RootJsonFormat[MessagePayload] = jsonFormat3(MessagePayload)
  implicit val uuidFormat: RootJsonFormat[UUID]              = new RootJsonFormat[UUID] {
    override def read(json: JsValue): UUID  = json match {
      case JsString(x) => UUID.fromString(x)
      case _           => throw new DeserializationException("UUID expected")
    }
    override def write(uuid: UUID): JsValue = JsString(uuid.toString)

  }
  implicit val messageFormat: RootJsonFormat[Message]        = jsonFormat5(Message.apply)

  def from(s: String): Either[Throwable, Message] = Try(s.parseJson.convertTo[Message]).toEither
}
