package it.pagopa.interop.notifier.service.converters

import it.pagopa.interop.agreementmanagement.model.agreement.{Active, PersistentAgreement}
import it.pagopa.interop.agreementmanagement.model.persistence.AgreementAdded
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.time.OffsetDateTime
import java.util.UUID

class AgreementEventsConverterSpec extends AnyWordSpecLike with Matchers with ScalaFutures {

  "Agreement event" should {

    "be properly converted" in {
      // given
      val producerId = UUID.randomUUID()

      val pa = PersistentAgreement(
        id = UUID.randomUUID(),
        eserviceId = UUID.randomUUID(),
        descriptorId = UUID.randomUUID(),
        producerId = producerId,
        consumerId = UUID.randomUUID(),
        state = Active,
        verifiedAttributes = Seq.empty,
        suspendedByConsumer = None,
        suspendedByProducer = None,
        createdAt = OffsetDateTime.now(),
        updatedAt = None
      )
      val ad = AgreementAdded(pa)

      // when
      val result: UUID = AgreementEventsConverter.getRecipient(ad).futureValue

      // then
      result shouldBe producerId

    }

  }

}
