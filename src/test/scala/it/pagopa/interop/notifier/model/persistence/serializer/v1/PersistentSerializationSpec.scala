package it.pagopa.interop.notifier.model.persistence.serializer.v1

import org.scalacheck.Prop.forAll
import org.scalacheck.Gen
import munit.ScalaCheckSuite
import PersistentSerializationSpec._
import it.pagopa.interop.notifier.model.persistence._
import it.pagopa.interop.notifier.model.persistence.serializer._
import it.pagopa.interop.notifier.model.persistence.serializer.v1.state._
import it.pagopa.interop.notifier.model.persistence.serializer.v1.events.EventIdAddedV1
import com.softwaremill.diffx.munit.DiffxAssertions
import com.softwaremill.diffx.generic.auto._
import com.softwaremill.diffx.Diff
import scala.reflect.runtime.universe.{typeOf, TypeTag}

class PersistentSerializationSpec extends ScalaCheckSuite with DiffxAssertions {

  serdeCheck[State, StateV1](stateGen, _.sorted)
  deserCheck[State, StateV1](stateGen)
  serdeCheck[EventIdAdded, EventIdAddedV1](eventIdAddedGen)
  deserCheck[EventIdAdded, EventIdAddedV1](eventIdAddedGen)

  // TODO move me in commons
  def serdeCheck[A: TypeTag, B](gen: Gen[(A, B)], adapter: B => B = identity[B](_))(implicit
    e: PersistEventSerializer[A, B],
    loc: munit.Location,
    d: => Diff[Either[Throwable, B]]
  ): Unit = property(s"${typeOf[A].typeSymbol.name.toString} is correctly serialized") {
    forAll(gen) { case (state, stateV1) =>
      implicit val diffX: Diff[Either[Throwable, B]] = d
      assertEqual(PersistEventSerializer.to[A, B](state).map(adapter), Right(stateV1).map(adapter))
    }
  }

  // TODO move me in commons
  def deserCheck[A, B: TypeTag](
    gen: Gen[(A, B)]
  )(implicit e: PersistEventDeserializer[B, A], loc: munit.Location, d: => Diff[Either[Throwable, A]]): Unit =
    property(s"${typeOf[B].typeSymbol.name.toString} is correctly serialized") {
      forAll(gen) { case (state, stateV1) =>
        // * This is declared lazy in the signature to avoid a MethodTooBigException
        implicit val diffX: Diff[Either[Throwable, A]] = d
        assertEqual(PersistEventDeserializer.from[B, A](stateV1), Right(state))
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

  implicit class PimpedStateV1(val stateV1: StateV1) extends AnyVal {
    def sorted: StateV1 = stateV1.copy(stateV1.identifiers.sortBy(_.key))
  }

  val eventIdAddedGen: Gen[(EventIdAdded, EventIdAddedV1)] = stateEntryGen.map { case (id, next) =>
    (EventIdAdded(id, next), EventIdAddedV1(id, next))
  }

}
