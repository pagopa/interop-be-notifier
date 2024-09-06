package it.pagopa.interop.notifier.database

import it.pagopa.interop.notifier.service.converters.EventType
import it.pagopa.interop.notifier.service.converters.EventType.EventType
import slick.jdbc.GetResult

final case class ProducerKeyEventRecord(eventId: Long, kid: String, eventType: EventType)

object ProducerKeyEventRecord {
  implicit val eventResult: GetResult[ProducerKeyEventRecord] =
    GetResult(r => ProducerKeyEventRecord(r.<<, r.<<, EventType.withName(r.<<)))
}
