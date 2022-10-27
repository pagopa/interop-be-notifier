package it.pagopa.interop.notifier.service.converters

import it.pagopa.interop.catalogmanagement.model.persistence._
import it.pagopa.interop.commons.queue.message.ProjectableEvent
import it.pagopa.interop.commons.utils.errors.ComponentError
import it.pagopa.interop.notifier.model.{EServicePayload, MessageId, NotificationPayload}
import it.pagopa.interop.notifier.service.converters.EventType._
import it.pagopa.interop.notifier.service.impl.DynamoNotificationResourcesService
import org.scanamo.ScanamoAsync

import scala.concurrent.{ExecutionContext, Future}

object CatalogEventsConverter {

  def getMessageId(dynamoService: DynamoNotificationResourcesService)(implicit
    scanamo: ScanamoAsync,
    ec: ExecutionContext,
    contexts: Seq[(String, String)]
  ): PartialFunction[ProjectableEvent, Future[MessageId]] = { case e: Event =>
    getMessageIdFromEvent(dynamoService, e)
  }

  private[this] def getMessageIdFromEvent(dynamoService: DynamoNotificationResourcesService, event: Event)(implicit
    scanamo: ScanamoAsync,
    ec: ExecutionContext,
    contexts: Seq[(String, String)]
  ): Future[MessageId] =
    event match {
      case CatalogItemAdded(c)                     =>
        val messageId: MessageId = MessageId(c.id, allOrganizations)
        dynamoService.put(messageId).map(_ => messageId)
      case ClonedCatalogItemAdded(c)               =>
        val messageId: MessageId = MessageId(c.id, allOrganizations)
        dynamoService.put(messageId).map(_ => messageId)
      case CatalogItemUpdated(c)                   => Future.successful(MessageId(c.id, allOrganizations))
      case CatalogItemWithDescriptorsDeleted(c, _) => Future.successful(MessageId(c.id, allOrganizations))
      case CatalogItemDocumentUpdated(id, _, _, _) => getMessageIdFromDynamo(dynamoService)(id)
      case CatalogItemDeleted(id)                  => getMessageIdFromDynamo(dynamoService)(id)
      case CatalogItemDocumentAdded(id, _, _, _)   => getMessageIdFromDynamo(dynamoService)(id)
      case CatalogItemDocumentDeleted(id, _, _)    => getMessageIdFromDynamo(dynamoService)(id)
      case CatalogItemDescriptorAdded(id, _)       => getMessageIdFromDynamo(dynamoService)(id)
      case CatalogItemDescriptorUpdated(id, _)     => getMessageIdFromDynamo(dynamoService)(id)
    }

  def asNotificationPayload: PartialFunction[ProjectableEvent, Either[ComponentError, NotificationPayload]] = {
    case e: Event =>
      Right(getEventNotificationPayload(e))
  }

  private[this] def getEventNotificationPayload(event: Event): NotificationPayload = {
    event match {
      case CatalogItemAdded(catalogItem)       => EServicePayload(catalogItem.id.toString(), None, ADDED.toString())
      case ClonedCatalogItemAdded(catalogItem) => EServicePayload(catalogItem.id.toString(), None, CLONED.toString())
      case CatalogItemUpdated(catalogItem)     => EServicePayload(catalogItem.id.toString(), None, UPDATED.toString())
      case CatalogItemWithDescriptorsDeleted(catalogItem, descriptorId) =>
        EServicePayload(catalogItem.id.toString(), Some(descriptorId), DELETED.toString())
      case CatalogItemDocumentUpdated(eServiceId, descriptorId, _, _)   =>
        EServicePayload(eServiceId, Some(descriptorId), UPDATED.toString())
      case CatalogItemDeleted(catalogItemId) => EServicePayload(catalogItemId, None, DELETED.toString())
      case CatalogItemDocumentAdded(eServiceId, descriptorId, _, _)    =>
        EServicePayload(eServiceId, Some(descriptorId), UPDATED.toString())
      case CatalogItemDocumentDeleted(eServiceId, descriptorId, _)     =>
        EServicePayload(eServiceId, Some(descriptorId), UPDATED.toString())
      case CatalogItemDescriptorAdded(eServiceId, catalogDescriptor)   =>
        EServicePayload(eServiceId, Some(catalogDescriptor.id.toString()), ADDED.toString())
      case CatalogItemDescriptorUpdated(eServiceId, catalogDescriptor) =>
        EServicePayload(eServiceId, Some(catalogDescriptor.id.toString()), UPDATED.toString())
    }

  }
}
