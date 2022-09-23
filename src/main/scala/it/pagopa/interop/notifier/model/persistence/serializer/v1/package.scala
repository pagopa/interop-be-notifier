package it.pagopa.interop.notifier.model.persistence.serializer

import cats.implicits._
import it.pagopa.interop.notifier.model.persistence._
import it.pagopa.interop.notifier.model.persistence.serializer.v1.events.EventIdAddedV1
import it.pagopa.interop.notifier.model.persistence.serializer.v1.state.{StateEntryV1, StateV1}

package object v1 {

  implicit def stateV1PersistEventDeserializer: PersistEventDeserializer[StateV1, State] =
    state => State(state.identifiers.map(entry => entry.key -> entry.value).toMap).asRight[Throwable]

  implicit def stateV1PersistEventSerializer: PersistEventSerializer[State, StateV1] =
    state => StateV1(state.identifiers.toSeq.map { case (k, v) => StateEntryV1(k, v) }).asRight[Throwable]

  implicit def eventIdAddedV1PersistEventDeserializer: PersistEventDeserializer[EventIdAddedV1, EventIdAdded] =
    event => EventIdAdded(event.organizationId, event.id).asRight[Throwable]

  implicit def eventIdAddedV1PersistEventSerializer: PersistEventSerializer[EventIdAdded, EventIdAddedV1] =
    event => EventIdAddedV1(event.organizationId, event.id).asRight[Throwable]
}
