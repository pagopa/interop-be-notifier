package it.pagopa.interop.notifier.service.converters

import it.pagopa.interop.catalogmanagement.model.persistence._
import it.pagopa.interop.catalogmanagement.model._
import it.pagopa.interop.commons.utils.errors.ComponentError
import it.pagopa.interop.notifier.model.{EServicePayload, NotificationPayload}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.time.OffsetDateTime
import java.util.UUID
import cats.syntax.all._

class CatalogEventsConverterSpec extends AnyWordSpecLike with Matchers with ScalaFutures {

  "Catalog conversions" should {

    "Convert catalog created to event payload" in {
      // given
      val id = UUID.randomUUID()
      val c  = getCatalogItem(id)
      val e  = CatalogItemAdded(c)

      // when
      val conversion: Either[ComponentError, Option[NotificationPayload]] =
        CatalogEventsConverter.asNotificationPayload(e)
      // then
      conversion shouldBe Right(EServicePayload(id.toString, None, EventType.ADDED.toString).some)
    }

    "Convert catalog cloned to event payload" in {
      // given
      val id = UUID.randomUUID()
      val c  = getCatalogItem(id)
      val e  = ClonedCatalogItemAdded(c)

      // when
      val conversion: Either[ComponentError, Option[NotificationPayload]] =
        CatalogEventsConverter.asNotificationPayload(e)
      // then
      conversion shouldBe Right(EServicePayload(id.toString, None, EventType.CLONED.toString).some)
    }

    "Convert catalog updated to event payload" in {
      // given
      val id = UUID.randomUUID()
      val c  = getCatalogItem(id)
      val e  = CatalogItemUpdated(c)

      // when
      val conversion: Either[ComponentError, Option[NotificationPayload]] =
        CatalogEventsConverter.asNotificationPayload(e)
      // then
      conversion shouldBe Right(EServicePayload(id.toString, None, EventType.UPDATED.toString).some)
    }

    "Convert catalog with descriptor delete to event payload" in {
      // given
      val id           = UUID.randomUUID()
      val descriptorId = UUID.randomUUID()
      val c            = getCatalogItem(id).copy(descriptors = Seq(getCatalogDescriptor(descriptorId)))
      val e            = CatalogItemWithDescriptorsDeleted(c, descriptorId.toString)

      // when
      val conversion: Either[ComponentError, Option[NotificationPayload]] =
        CatalogEventsConverter.asNotificationPayload(e)
      // then
      conversion shouldBe Right(
        EServicePayload(id.toString, Some(descriptorId.toString), EventType.DELETED.toString).some
      )
    }

    "Convert catalog document updated to event payload" in {
      // given
      val id           = UUID.randomUUID()
      val descriptorId = UUID.randomUUID()
      val documentId   = UUID.randomUUID()
      val document     = getCatalogDocument(documentId)
      val e = CatalogItemDocumentUpdated(id.toString, descriptorId.toString, documentId.toString, document, Nil)

      // when
      val conversion: Either[ComponentError, Option[NotificationPayload]] =
        CatalogEventsConverter.asNotificationPayload(e)
      // then
      conversion shouldBe Right(
        EServicePayload(id.toString, Some(descriptorId.toString), EventType.UPDATED.toString).some
      )
    }

    "Convert catalog deleted to event payload" in {
      // given
      val id = UUID.randomUUID()

      val e = CatalogItemDeleted(id.toString)

      // when
      val conversion: Either[ComponentError, Option[NotificationPayload]] =
        CatalogEventsConverter.asNotificationPayload(e)
      // then
      conversion shouldBe Right(EServicePayload(id.toString, None, EventType.DELETED.toString).some)
    }

    "Convert catalog document added to event payload" in {
      // given
      val id           = UUID.randomUUID()
      val descriptorId = UUID.randomUUID()
      val documentId   = UUID.randomUUID()
      val document     = getCatalogDocument(documentId)
      val e            = CatalogItemDocumentAdded(id.toString, descriptorId.toString, document, true, Nil)

      // when
      val conversion: Either[ComponentError, Option[NotificationPayload]] =
        CatalogEventsConverter.asNotificationPayload(e)
      // then
      conversion shouldBe Right(
        EServicePayload(id.toString, Some(descriptorId.toString), EventType.UPDATED.toString).some
      )
    }

    "Convert catalog document deleted to event payload" in {
      // given
      val id           = UUID.randomUUID()
      val descriptorId = UUID.randomUUID()
      val documentId   = UUID.randomUUID()
      val e            = CatalogItemDocumentDeleted(id.toString, descriptorId.toString, documentId.toString)

      // when
      val conversion: Either[ComponentError, Option[NotificationPayload]] =
        CatalogEventsConverter.asNotificationPayload(e)
      // then
      conversion shouldBe Right(
        EServicePayload(id.toString, Some(descriptorId.toString), EventType.UPDATED.toString).some
      )
    }

    "Convert catalog descriptor added to event payload" in {
      // given
      val id           = UUID.randomUUID()
      val descriptorId = UUID.randomUUID()
      val descriptor   = getCatalogDescriptor(descriptorId)
      val e            = CatalogItemDescriptorAdded(id.toString, descriptor)

      // when
      val conversion: Either[ComponentError, Option[NotificationPayload]] =
        CatalogEventsConverter.asNotificationPayload(e)
      // then
      conversion shouldBe Right(
        EServicePayload(id.toString, Some(descriptorId.toString), EventType.ADDED.toString).some
      )
    }

    "Convert catalog descriptor updated to event payload" in {
      // given
      val id           = UUID.randomUUID()
      val descriptorId = UUID.randomUUID()
      val descriptor   = getCatalogDescriptor(descriptorId)
      val e            = CatalogItemDescriptorUpdated(id.toString, descriptor)

      // when
      val conversion: Either[ComponentError, Option[NotificationPayload]] =
        CatalogEventsConverter.asNotificationPayload(e)
      // then
      conversion shouldBe Right(
        EServicePayload(id.toString, Some(descriptorId.toString), EventType.UPDATED.toString).some
      )
    }

  }

  private def getCatalogItem(id: UUID) = CatalogItem(
    id = id,
    producerId = UUID.randomUUID(),
    name = "eservice name",
    description = "eservice description",
    technology = Rest,
    attributes = None,
    descriptors = Seq.empty,
    createdAt = OffsetDateTime.now(),
    mode = CatalogItemMode.default,
    riskAnalysis = Seq.empty
  )

  private def getCatalogDescriptor(id: UUID) = CatalogDescriptor(
    id = id,
    version = "v1",
    description = None,
    interface = None,
    docs = Seq.empty,
    state = Published,
    audience = Seq.empty,
    voucherLifespan = 1,
    dailyCallsPerConsumer = 1,
    dailyCallsTotal = 1,
    agreementApprovalPolicy = None,
    serverUrls = List("url"),
    createdAt = OffsetDateTime.now().minusDays(10),
    archivedAt = None,
    publishedAt = Some(OffsetDateTime.now()),
    suspendedAt = None,
    deprecatedAt = None,
    attributes = CatalogAttributes(certified = Seq.empty, declared = Seq.empty, verified = Seq.empty)
  )

  private def getCatalogDocument(id: UUID) = CatalogDocument(
    id = id,
    name = "name",
    contentType = "contentType",
    prettyName = "prettyName",
    path = "path",
    checksum = "checksum",
    uploadDate = OffsetDateTime.now()
  )

}
