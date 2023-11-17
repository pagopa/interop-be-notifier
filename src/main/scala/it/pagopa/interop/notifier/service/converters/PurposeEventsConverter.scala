package it.pagopa.interop.notifier.service.converters

import it.pagopa.interop.commons.queue.message.ProjectableEvent
import it.pagopa.interop.commons.utils.errors.ComponentError
import it.pagopa.interop.notifier.model.{MessageId, NotificationPayload, PurposePayload}
import it.pagopa.interop.notifier.service.CatalogManagementService
import it.pagopa.interop.notifier.service.converters.EventType._
import it.pagopa.interop.notifier.service.impl.DynamoNotificationResourcesService
import it.pagopa.interop.purposemanagement.model.persistence._
import cats.syntax.all._

import scala.concurrent.{ExecutionContext, Future}

object PurposeEventsConverter {
  def getMessageId(
    catalogManagementService: CatalogManagementService,
    dynamoIndexService: DynamoNotificationResourcesService
  )(implicit
    ec: ExecutionContext,
    contexts: Seq[(String, String)]
  ): PartialFunction[ProjectableEvent, Future[Seq[MessageId]]] = { case e: Event =>
    getMessageIdFromEvent(catalogManagementService, dynamoIndexService, e)
  }

  private[this] def getMessageIdFromEvent(
    catalogManagementService: CatalogManagementService,
    dynamoIndexService: DynamoNotificationResourcesService,
    event: Event
  )(implicit ec: ExecutionContext, contexts: Seq[(String, String)]): Future[Seq[MessageId]] =
    event match {
      case PurposeCreated(purpose) =>
        for {
          messageId <- catalogManagementService
            .getEServiceProducerByEServiceId(purpose.eserviceId)
            .map(organizationId => MessageId(purpose.id, organizationId.toString()))
          _         <- dynamoIndexService.put(messageId)
        } yield Seq(messageId)
      case PurposeUpdated(purpose) => getMessageIdFromDynamo(dynamoIndexService)(purpose.id.toString()).map(Seq(_))
      case PurposeVersionCreated(purposeId, _)      => getMessageIdFromDynamo(dynamoIndexService)(purposeId).map(Seq(_))
      case PurposeVersionActivated(purpose)         =>
        getMessageIdFromDynamo(dynamoIndexService)(purpose.id.toString()).map(Seq(_))
      case PurposeVersionSuspended(purpose)         =>
        getMessageIdFromDynamo(dynamoIndexService)(purpose.id.toString()).map(Seq(_))
      case PurposeVersionWaitedForApproval(purpose) =>
        getMessageIdFromDynamo(dynamoIndexService)(purpose.id.toString()).map(Seq(_))
      case PurposeVersionArchived(purpose)          =>
        getMessageIdFromDynamo(dynamoIndexService)(purpose.id.toString()).map(Seq(_))
      case PurposeVersionUpdated(purposeId, _)      => getMessageIdFromDynamo(dynamoIndexService)(purposeId).map(Seq(_))
      case PurposeVersionDeleted(purposeId, _)      => getMessageIdFromDynamo(dynamoIndexService)(purposeId).map(Seq(_))
      case PurposeDeleted(purposeId)                => getMessageIdFromDynamo(dynamoIndexService)(purposeId).map(Seq(_))
    }

  def asNotificationPayload: PartialFunction[ProjectableEvent, Either[ComponentError, Option[NotificationPayload]]] = {
    case e: Event =>
      Right(getEventNotificationPayload(e))
  }

  private[this] def getEventNotificationPayload(event: Event): Option[NotificationPayload] =
    event match {
      // Creates Drafts, should not be notified
      case _: PurposeCreated                        => None
      // Only on Drafts, should not be notified
      case _: PurposeUpdated                        => None
      // Creates Drafts, should not be notified
      case _: PurposeVersionCreated                 => None
      case PurposeVersionActivated(purpose)         => PurposePayload(purpose.id.toString(), ACTIVATED.toString()).some
      case PurposeVersionSuspended(purpose)         => PurposePayload(purpose.id.toString(), SUSPENDED.toString()).some
      case PurposeVersionWaitedForApproval(purpose) =>
        PurposePayload(purpose.id.toString(), WAITING_FOR_APPROVAL.toString).some
      case PurposeVersionArchived(purpose)          => PurposePayload(purpose.id.toString(), ARCHIVED.toString()).some
      // Only on Drafts, should not be notified
      case _: PurposeVersionUpdated                 => None
      // Only on Drafts, should not be notified
      case _: PurposeVersionDeleted                 => None
      // Only on Drafts, should not be notified
      case _: PurposeDeleted                        => None
    }
}
