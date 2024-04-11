package it.pagopa.interop.notifier.service.converters

import it.pagopa.interop.commons.queue.message.ProjectableEvent
import it.pagopa.interop.commons.utils.errors.ComponentError
import it.pagopa.interop.notifier.model.{MessageId, NotificationPayload, PurposePayload}
import it.pagopa.interop.notifier.service.CatalogProcessService
import it.pagopa.interop.notifier.service.converters.EventType._
import it.pagopa.interop.notifier.service.impl.DynamoNotificationResourcesService
import it.pagopa.interop.purposemanagement.model.persistence._
import cats.syntax.all._

import scala.concurrent.{ExecutionContext, Future}

object PurposeEventsConverter {
  def getMessageId(
    catalogProcessService: CatalogProcessService,
    dynamoIndexService: DynamoNotificationResourcesService
  )(implicit
    ec: ExecutionContext,
    contexts: Seq[(String, String)]
  ): PartialFunction[ProjectableEvent, Future[Option[MessageId]]] = { case e: Event =>
    getMessageIdFromEvent(catalogProcessService, dynamoIndexService, e)
  }

  private[this] def getMessageIdFromEvent(
    catalogProcessService: CatalogProcessService,
    dynamoIndexService: DynamoNotificationResourcesService,
    event: Event
  )(implicit ec: ExecutionContext, contexts: Seq[(String, String)]): Future[Option[MessageId]] =
    event match {
      case PurposeCreated(purpose) =>
        for {
          messageId <- catalogProcessService
            .getEServiceProducerByEServiceId(purpose.eserviceId)
            .map(organizationId => MessageId(purpose.id, organizationId.toString()))
          _         <- dynamoIndexService.put(messageId)
        } yield messageId.some
      case PurposeUpdated(purpose) => getMessageIdFromDynamo(dynamoIndexService)(purpose.id.toString()).map(_.some)
      case PurposeVersionCreated(purposeId, _)      => getMessageIdFromDynamo(dynamoIndexService)(purposeId).map(_.some)
      case PurposeVersionActivated(purpose)         =>
        getMessageIdFromDynamo(dynamoIndexService)(purpose.id.toString()).map(_.some)
      case PurposeVersionSuspended(purpose)         =>
        getMessageIdFromDynamo(dynamoIndexService)(purpose.id.toString()).map(_.some)
      case PurposeVersionWaitedForApproval(purpose) =>
        getMessageIdFromDynamo(dynamoIndexService)(purpose.id.toString()).map(_.some)
      case PurposeVersionArchived(purpose)          =>
        getMessageIdFromDynamo(dynamoIndexService)(purpose.id.toString()).map(_.some)
      case PurposeVersionUpdated(purposeId, _)      => getMessageIdFromDynamo(dynamoIndexService)(purposeId).map(_.some)
      case PurposeVersionDeleted(purposeId, _)      => getMessageIdFromDynamo(dynamoIndexService)(purposeId).map(_.some)
      case PurposeDeleted(purposeId)                => getMessageIdFromDynamo(dynamoIndexService)(purposeId).map(_.some)
      case PurposeVersionRejected(purpose, _)       =>
        getMessageIdFromDynamo(dynamoIndexService)(purpose.id.toString).map(_.some)
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
      case PurposeVersionRejected(purpose, _)       => PurposePayload(purpose.id.toString(), REJECTED.toString()).some
      // Only on Drafts, should not be notified
      case _: PurposeVersionUpdated                 => None
      // Only on Drafts, should not be notified
      case _: PurposeVersionDeleted                 => None
      // Only on Drafts, should not be notified
      case _: PurposeDeleted                        => None
    }
}
