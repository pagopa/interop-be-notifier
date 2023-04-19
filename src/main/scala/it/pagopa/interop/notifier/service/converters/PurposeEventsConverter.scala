package it.pagopa.interop.notifier.service.converters

import it.pagopa.interop.commons.queue.message.ProjectableEvent
import it.pagopa.interop.commons.utils.errors.ComponentError
import it.pagopa.interop.notifier.model.{MessageId, NotificationPayload, PurposePayload}
import it.pagopa.interop.notifier.service.CatalogManagementService
import it.pagopa.interop.notifier.service.converters.EventType._
import it.pagopa.interop.notifier.service.impl.DynamoNotificationResourcesService
import it.pagopa.interop.purposemanagement.model.persistence._

import scala.concurrent.{ExecutionContext, Future}

object PurposeEventsConverter {
  def getMessageId(
    catalogManagementService: CatalogManagementService,
    dynamoIndexService: DynamoNotificationResourcesService
  )(implicit
    ec: ExecutionContext,
    contexts: Seq[(String, String)]
  ): PartialFunction[ProjectableEvent, Future[MessageId]] = { case e: Event =>
    getMessageIdFromEvent(catalogManagementService, dynamoIndexService, e)
  }

  private[this] def getMessageIdFromEvent(
    catalogManagementService: CatalogManagementService,
    dynamoIndexService: DynamoNotificationResourcesService,
    event: Event
  )(implicit ec: ExecutionContext, contexts: Seq[(String, String)]): Future[MessageId] =
    event match {
      case PurposeCreated(purpose)                  =>
        for {
          messageId <- catalogManagementService
            .getEServiceProducerByEServiceId(purpose.eserviceId)
            .map(organizationId => MessageId(purpose.id, organizationId.toString()))
          _         <- dynamoIndexService.put(messageId)
        } yield messageId
      case PurposeUpdated(purpose)                  => getMessageIdFromDynamo(dynamoIndexService)(purpose.id.toString())
      case PurposeVersionCreated(purposeId, _)      => getMessageIdFromDynamo(dynamoIndexService)(purposeId)
      case PurposeVersionActivated(purpose)         => getMessageIdFromDynamo(dynamoIndexService)(purpose.id.toString())
      case PurposeVersionSuspended(purpose)         => getMessageIdFromDynamo(dynamoIndexService)(purpose.id.toString())
      case PurposeVersionWaitedForApproval(purpose) => getMessageIdFromDynamo(dynamoIndexService)(purpose.id.toString())
      case PurposeVersionArchived(purpose)          => getMessageIdFromDynamo(dynamoIndexService)(purpose.id.toString())
      case PurposeVersionUpdated(purposeId, _)      => getMessageIdFromDynamo(dynamoIndexService)(purposeId)
      case PurposeVersionDeleted(purposeId, _)      => getMessageIdFromDynamo(dynamoIndexService)(purposeId)
      case PurposeDeleted(purposeId)                => getMessageIdFromDynamo(dynamoIndexService)(purposeId)
    }

  def asNotificationPayload: PartialFunction[ProjectableEvent, Either[ComponentError, NotificationPayload]] = {
    case e: Event =>
      Right(getEventNotificationPayload(e))
  }

  private[this] def getEventNotificationPayload(event: Event): NotificationPayload =
    event match {
      case PurposeCreated(purpose)                  => PurposePayload(purpose.id.toString(), CREATED.toString())
      case PurposeUpdated(purpose)                  => PurposePayload(purpose.id.toString(), UPDATED.toString())
      case PurposeVersionCreated(purposeId, _)      => PurposePayload(purposeId, CREATED.toString)
      case PurposeVersionActivated(purpose)         => PurposePayload(purpose.id.toString(), ACTIVATED.toString())
      case PurposeVersionSuspended(purpose)         => PurposePayload(purpose.id.toString(), SUSPENDED.toString())
      case PurposeVersionWaitedForApproval(purpose) =>
        PurposePayload(purpose.id.toString(), WAITING_FOR_APPROVAL.toString)
      case PurposeVersionArchived(purpose)          => PurposePayload(purpose.id.toString(), ARCHIVED.toString())
      case PurposeVersionUpdated(purposeId, _)      => PurposePayload(purposeId, UPDATED.toString())
      case PurposeVersionDeleted(purposeId, _)      => PurposePayload(purposeId, DELETED.toString())
      case PurposeDeleted(purposeId)                => PurposePayload(purposeId, DELETED.toString())
    }
}
