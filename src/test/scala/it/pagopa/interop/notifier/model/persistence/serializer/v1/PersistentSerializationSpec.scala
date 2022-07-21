package it.pagopa.interop.notifier.model.persistence.serializer.v1

import org.scalacheck.Prop.forAll
import org.scalacheck.Gen
import munit.ScalaCheckSuite
import PersistentSerializationSpec._
import it.pagopa.interop.notifier.model.persistence._
import it.pagopa.interop.notifier.model.persistence.serializer._
import it.pagopa.interop.notifier.model.persistence.serializer.v1.state._
import it.pagopa.interop.notifier.model.persistence.serializer.v1.events.EventIdAddedV1

class PersistentSerializationSpec extends ScalaCheckSuite {

  property("State is correctly deserialized") {
    forAll(stateGen) { case (state, stateV1) =>
      PersistEventDeserializer.from[StateV1, State](stateV1) == Right(state)
    }
  }

  property("State is correctly serialized") {
    forAll(stateGen) { case (state, stateV1) =>
      PersistEventSerializer.to[State, StateV1](state) == Right(stateV1)
    }
  }

  property("EventIdAdded is correctly deserialized") {
    forAll(eventIdAddedGen) { case (state, stateV1) =>
      PersistEventDeserializer.from[EventIdAddedV1, EventIdAdded](stateV1) == Right(state)
    }
  }

  property("EventIdAdded is correctly serialized") {
    forAll(eventIdAddedGen) { case (state, stateV1) =>
      PersistEventSerializer.to[EventIdAdded, EventIdAddedV1](state) == Right(stateV1)
    }
  }

}

object PersistentSerializationSpec {

  val stateEntryGen: Gen[(String, Long)] = for {
    organizationId <- Gen.alphaNumStr
    nextId         <- Gen.posNum[Long]
  } yield (organizationId, nextId)

  val stateGen: Gen[(State, StateV1)] = for {
    map <- Gen.mapOf(stateEntryGen)
    stateEntryList = map.toList.map { case (k, v) => StateEntryV1(k, v) }
  } yield (State(map), StateV1(stateEntryList))

  val eventIdAddedGen: Gen[(EventIdAdded, EventIdAddedV1)] = stateEntryGen.map { case (id, next) =>
    (EventIdAdded(id, next), EventIdAddedV1(id, next))
  }

}
