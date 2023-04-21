package it.pagopa.interop.notifier.database

import it.pagopa.interop.notifier.service.converters.EventType
import it.pagopa.interop.notifier.service.converters.EventType.EventType
import slick.jdbc.GetResult

final case class KeyEventRecord(eventId: Long, kid: String, eventType: EventType)

object KeyEventRecord {
  implicit val eventResult: GetResult[KeyEventRecord] =
    GetResult(r => KeyEventRecord(r.<<, r.<<, EventType.withName(r.<<)))
}
