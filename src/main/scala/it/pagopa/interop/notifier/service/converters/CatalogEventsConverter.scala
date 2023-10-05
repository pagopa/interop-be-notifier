package it.pagopa.interop.notifier.service.converters

import it.pagopa.interop.catalogmanagement.model.persistence._
import it.pagopa.interop.commons.queue.message.ProjectableEvent
import it.pagopa.interop.commons.utils.errors.ComponentError
import it.pagopa.interop.notifier.model.{EServicePayload, MessageId, NotificationPayload}
import it.pagopa.interop.notifier.service.converters.EventType._
import it.pagopa.interop.notifier.service.impl.DynamoNotificationResourcesService

import scala.concurrent.{ExecutionContext, Future}
import it.pagopa.interop.notifier.service.converters.allOrganizations
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
      case CatalogItemAdded(catalogItem)       => EServicePayload(catalogItem.id.toString, None, ADDED.toString).some
      case ClonedCatalogItemAdded(catalogItem) => EServicePayload(catalogItem.id.toString, None, CLONED.toString).some
      case CatalogItemUpdated(catalogItem)     => EServicePayload(catalogItem.id.toString, None, UPDATED.toString).some
      case CatalogItemWithDescriptorsDeleted(catalogItem, descriptorId)  =>
        EServicePayload(catalogItem.id.toString, Some(descriptorId), DELETED.toString).some
      case CatalogItemDocumentUpdated(eServiceId, descriptorId, _, _, _) =>
        EServicePayload(eServiceId, Some(descriptorId), UPDATED.toString).some
      case CatalogItemDeleted(catalogItemId) => EServicePayload(catalogItemId, None, DELETED.toString).some
      case CatalogItemDocumentAdded(eServiceId, descriptorId, _, _, _) =>
        EServicePayload(eServiceId, Some(descriptorId), UPDATED.toString).some
      case CatalogItemDocumentDeleted(eServiceId, descriptorId, _)     =>
        EServicePayload(eServiceId, Some(descriptorId), UPDATED.toString).some
      case CatalogItemDescriptorAdded(eServiceId, catalogDescriptor)   =>
        EServicePayload(eServiceId, Some(catalogDescriptor.id.toString), ADDED.toString).some
      case CatalogItemDescriptorUpdated(eServiceId, catalogDescriptor) =>
        EServicePayload(eServiceId, Some(catalogDescriptor.id.toString), UPDATED.toString).some
      case CatalogItemRiskAnalysisAdded(catalogItem, _)                =>
        EServicePayload(catalogItem.id.toString, None, UPDATED.toString).some
      case CatalogItemRiskAnalysisDeleted(catalogItem, _)              =>
        EServicePayload(catalogItem.id.toString, None, UPDATED.toString).some
      case CatalogItemRiskAnalysisUpdated(catalogItem, _)              =>
        EServicePayload(catalogItem.id.toString, None, UPDATED.toString).some
      case MovedAttributesFromEserviceToDescriptors(_)                 => None

    }

}
