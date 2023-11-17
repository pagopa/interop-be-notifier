package it.pagopa.interop.notifier.service.converters

import it.pagopa.interop.catalogmanagement.model.persistence._
import it.pagopa.interop.commons.queue.message.ProjectableEvent
import it.pagopa.interop.commons.utils.errors.ComponentError
import it.pagopa.interop.notifier.model.{EServicePayload, MessageId, NotificationPayload}
import it.pagopa.interop.notifier.service.converters.EventType._
import it.pagopa.interop.notifier.service.impl.DynamoNotificationResourcesService

import scala.concurrent.{ExecutionContext, Future}
import cats.syntax.all._

object CatalogEventsConverter {

  def getMessageId(dynamoService: DynamoNotificationResourcesService)(implicit
    ec: ExecutionContext,
    contexts: Seq[(String, String)]
  ): PartialFunction[ProjectableEvent, Future[Seq[MessageId]]] = { case e: Event =>
    getMessageIdFromEvent(dynamoService, e)
  }

  private[this] def getMessageIdFromEvent(dynamoService: DynamoNotificationResourcesService, event: Event)(implicit
    ec: ExecutionContext,
    contexts: Seq[(String, String)]
  ): Future[Seq[MessageId]] =
    event match {
      case CatalogItemAdded(c)                         =>
        val messageId: MessageId = MessageId(c.id, allOrganizations)
        dynamoService.put(messageId).map(_ => Seq(messageId))
      case ClonedCatalogItemAdded(c)                   =>
        val messageId: MessageId = MessageId(c.id, allOrganizations)
        dynamoService.put(messageId).map(_ => Seq(messageId))
      case CatalogItemUpdated(c)                       => Future.successful(Seq(MessageId(c.id, allOrganizations)))
      case CatalogItemWithDescriptorsDeleted(c, _)     => Future.successful(Seq(MessageId(c.id, allOrganizations)))
      case CatalogItemDocumentUpdated(id, _, _, _, _)  => getMessageIdFromDynamo(dynamoService)(id).map(Seq(_))
      case CatalogItemDeleted(id)                      => getMessageIdFromDynamo(dynamoService)(id).map(Seq(_))
      case CatalogItemDocumentAdded(id, _, _, _, _)    => getMessageIdFromDynamo(dynamoService)(id).map(Seq(_))
      case CatalogItemDocumentDeleted(id, _, _)        => getMessageIdFromDynamo(dynamoService)(id).map(Seq(_))
      case CatalogItemDescriptorAdded(id, _)           => getMessageIdFromDynamo(dynamoService)(id).map(Seq(_))
      case CatalogItemDescriptorUpdated(id, _)         => getMessageIdFromDynamo(dynamoService)(id).map(Seq(_))
      case CatalogItemRiskAnalysisAdded(c, _)          => Future.successful(Seq(MessageId(c.id, allOrganizations)))
      case CatalogItemRiskAnalysisDeleted(c, _)        => Future.successful(Seq(MessageId(c.id, allOrganizations)))
      case CatalogItemRiskAnalysisUpdated(c, _)        => Future.successful(Seq(MessageId(c.id, allOrganizations)))
      case MovedAttributesFromEserviceToDescriptors(_) => Future.successful(Seq())
    }

  def asNotificationPayload: PartialFunction[ProjectableEvent, Either[ComponentError, Option[NotificationPayload]]] = {
    case e: Event => Right(getEventNotificationPayload(e))
  }

  private[this] def getEventNotificationPayload(event: Event): Option[NotificationPayload] =
    event match {
      // Empty EService created, should not be notified
      case _: CatalogItemAdded                                  => None
      // Creates a Draft Descriptor, should not be notified
      case _: ClonedCatalogItemAdded                            => None
      // Updates Drafts, should not be notified
      case _: CatalogItemUpdated                                => None
      // Deleted Drafts, should not be notified
      case _: CatalogItemWithDescriptorsDeleted                 => None
      // Document should be updated only on Drafts
      case _: CatalogItemDocumentUpdated                        => None
      // Deleted Drafts, should not be notified
      case _: CatalogItemDeleted                                => None
      // Only on Drafts, should not be notified
      case _: CatalogItemDocumentAdded                          => None
      // Only on Drafts, should not be notified
      case _: CatalogItemDocumentDeleted                        => None
      // Creates a Drafts, should not be notified
      case _: CatalogItemDescriptorAdded                        => None
      case CatalogItemDescriptorUpdated(eserviceId, descriptor) =>
        // Triggered for Publish, Suspension, Activation, Archive (but also for a rollback to Draft)
        EServicePayload(eserviceId, descriptor.id.toString.some, UPDATED.toString).some
      // Only on Drafts, should not be notified
      case _: CatalogItemRiskAnalysisAdded                      => None
      // Only on Drafts, should not be notified
      case _: CatalogItemRiskAnalysisDeleted                    => None
      // Only on Drafts, should not be notified
      case _: CatalogItemRiskAnalysisUpdated                    => None
      case _: MovedAttributesFromEserviceToDescriptors          => None

    }

}
