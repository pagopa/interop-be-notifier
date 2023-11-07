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
  ): PartialFunction[ProjectableEvent, Future[Option[MessageId]]] = { case e: Event =>
    getMessageIdFromEvent(dynamoService, e)
  }

  private[this] def getMessageIdFromEvent(dynamoService: DynamoNotificationResourcesService, event: Event)(implicit
    ec: ExecutionContext,
    contexts: Seq[(String, String)]
  ): Future[Option[MessageId]] =
    event match {
      case CatalogItemAdded(c)                         =>
        val messageId: MessageId = MessageId(c.id, allOrganizations)
        dynamoService.put(messageId).map(_ => messageId.some)
      case ClonedCatalogItemAdded(c)                   =>
        val messageId: MessageId = MessageId(c.id, allOrganizations)
        dynamoService.put(messageId).map(_ => messageId.some)
      case CatalogItemUpdated(c)                       => Future.successful(MessageId(c.id, allOrganizations).some)
      case CatalogItemWithDescriptorsDeleted(c, _)     => Future.successful(MessageId(c.id, allOrganizations).some)
      case CatalogItemDocumentUpdated(id, _, _, _, _)  => getMessageIdFromDynamo(dynamoService)(id).map(_.some)
      case CatalogItemDeleted(id)                      => getMessageIdFromDynamo(dynamoService)(id).map(_.some)
      case CatalogItemDocumentAdded(id, _, _, _, _)    => getMessageIdFromDynamo(dynamoService)(id).map(_.some)
      case CatalogItemDocumentDeleted(id, _, _)        => getMessageIdFromDynamo(dynamoService)(id).map(_.some)
      case CatalogItemDescriptorAdded(id, _)           => getMessageIdFromDynamo(dynamoService)(id).map(_.some)
      case CatalogItemDescriptorUpdated(id, _)         => getMessageIdFromDynamo(dynamoService)(id).map(_.some)
      case CatalogItemRiskAnalysisAdded(c, _)          => Future.successful(MessageId(c.id, allOrganizations).some)
      case CatalogItemRiskAnalysisDeleted(c, _)        => Future.successful(MessageId(c.id, allOrganizations).some)
      case CatalogItemRiskAnalysisUpdated(c, _)        => Future.successful(MessageId(c.id, allOrganizations).some)
      case MovedAttributesFromEserviceToDescriptors(_) => Future.successful(None)
    }

  def asNotificationPayload: PartialFunction[ProjectableEvent, Either[ComponentError, Option[NotificationPayload]]] = {
    case e: Event => Right(getEventNotificationPayload(e))
  }

  private[this] def getEventNotificationPayload(event: Event): Option[NotificationPayload] =
    event match {
      case CatalogItemDescriptorUpdated(eserviceId, _) =>
        // Triggered for Publish, Suspension, Activation, Archive (but also for a rollback to Draft)
        EServicePayload(eserviceId, None, UPDATED.toString).some
      case _: CatalogItemAdded                         =>
        // Empty EService created, should not be notified
        None
      case _: ClonedCatalogItemAdded                   =>
        // Creates a Draft Descriptor, should not be notified
        None
      case _: CatalogItemUpdated                       =>
        // Updates Drafts, should not be notified
        None
      case _: CatalogItemWithDescriptorsDeleted        =>
        // Deleted Drafts, should not be notified
        None
      case _: CatalogItemDocumentUpdated               =>
        // Document should be updated only on Drafts
        None
      case _: CatalogItemDeleted                       =>
        // Deleted Drafts, should not be notified
        None
      case _: CatalogItemDocumentAdded                 =>
        // Only on Drafts, should not be notified
        None
      case _: CatalogItemDocumentDeleted               =>
        // Only on Drafts, should not be notified
        None
      case _: CatalogItemDescriptorAdded               =>
        // Creates a Drafts, should not be notified
        None
      case _: CatalogItemDescriptorUpdated             =>
        // Only on Drafts, should not be notified
        None
      case _: CatalogItemRiskAnalysisAdded             =>
        // Only on Drafts, should not be notified
        None
      case _: CatalogItemRiskAnalysisDeleted           =>
        // Only on Drafts, should not be notified
        None
      case _: CatalogItemRiskAnalysisUpdated           =>
        // Only on Drafts, should not be notified
        None
      case _: MovedAttributesFromEserviceToDescriptors => None

    }

}
