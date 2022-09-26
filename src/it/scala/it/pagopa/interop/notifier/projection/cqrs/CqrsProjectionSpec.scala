package it.pagopa.interop.notifier.projection.cqrs

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import it.pagopa.interop.notifier.{ItSpecConfiguration, ItSpecHelper}
import spray.json._

import java.util.UUID
class CqrsProjectionSpec extends ScalaTestWithActorTestKit(ItSpecConfiguration.config) with ItSpecHelper {

  "Projection" should {
    "succeed for event EventIdAdded on missing organization" in {
      val organizationId = UUID.randomUUID()

      val result = updateOrganizationEventId(organizationId)

      assert(result.nonEmpty)

      val expected =
        JsObject(("id", JsString(organizationId.toString)), ("eventId", JsNumber(result.get.eventId)))

      val persisted = findOne[JsObject](organizationId.toString).futureValue

      expected shouldBe persisted
    }

    "succeed for event EventIdAdded on existing organization" in {
      val organizationId = UUID.randomUUID()

      updateOrganizationEventId(organizationId)
      val result = updateOrganizationEventId(organizationId)

      assert(result.nonEmpty)

      val expected =
        JsObject(("id", JsString(organizationId.toString)), ("eventId", JsNumber(result.get.eventId)))

      val persisted = findOne[JsObject](organizationId.toString).futureValue

      expected shouldBe persisted
    }

  }

}
