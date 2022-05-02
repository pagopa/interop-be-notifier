package it.pagopa.interop.notifier.model.persistence.serializer

import it.pagopa.interop.notifier.model.persistence._
import it.pagopa.interop.notifier.model.persistence.serializer.v1.events.EventIdAddedV1
import it.pagopa.interop.notifier.model.persistence.serializer.v1.state.{StateEntryV1, StateV1}

package object v1 {

  type ErrorOr[A] = Either[Throwable, A]

  implicit def stateV1PersistEventDeserializer: PersistEventDeserializer[StateV1, State] =
    state => {
      Right[Throwable, State](State(state.identifiers.map(entry => entry.key -> entry.value).toMap))
    }

  implicit def stateV1PersistEventSerializer: PersistEventSerializer[State, StateV1] =
    state => {
      val entries = state.identifiers.toSeq.map { case (k, v) =>
        StateEntryV1(k, v)
      }
      Right[Throwable, StateV1](StateV1(entries))
    }

  implicit def eventIdAddedV1PersistEventDeserializer: PersistEventDeserializer[EventIdAddedV1, EventIdAdded] =
    event => Right[Throwable, EventIdAdded](EventIdAdded(event.organizationId, event.id))

  implicit def eventIdAddedV1PersistEventSerializer: PersistEventSerializer[EventIdAdded, EventIdAddedV1] =
    event => Right[Throwable, EventIdAddedV1](EventIdAddedV1(event.organizationId, event.id))

}
