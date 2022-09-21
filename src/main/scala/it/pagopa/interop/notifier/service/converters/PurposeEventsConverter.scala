package it.pagopa.interop.notifier.service.converters

import it.pagopa.interop.commons.queue.message.ProjectableEvent
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.commons.utils.errors.ComponentError
import it.pagopa.interop.notifier.model.persistence.MessageId
import it.pagopa.interop.notifier.model.{DynamoEventPayload, PurposeEventPayload}
import it.pagopa.interop.notifier.service.converters.EventType._
import it.pagopa.interop.notifier.service.{CatalogManagementService, DynamoService}
import it.pagopa.interop.purposemanagement.model.persistence._

import scala.concurrent.{ExecutionContext, Future}

object PurposeEventsConverter {
  def getMessageId(catalogManagementService: CatalogManagementService, dynamoService: DynamoService)(implicit
    ec: ExecutionContext,
    contexts: Seq[(String, String)]
  ): PartialFunction[ProjectableEvent, Future[MessageId]] = { case e: Event =>
    getMessageId(catalogManagementService, dynamoService, e)
  }

  private[this] def getMessageId(
    catalogManagementService: CatalogManagementService,
    dynamoService: DynamoService,
    event: Event
  )(implicit ec: ExecutionContext, contexts: Seq[(String, String)]): Future[MessageId] = event match {
    case PurposeCreated(purpose)                  =>
      catalogManagementService
        .getEServiceProducerByEServiceId(purpose.eserviceId)
        .map(organizationId => MessageId(purpose.id, organizationId))
    case PurposeUpdated(purpose)                  => createMessageId(dynamoService)(purpose.id)
    case PurposeVersionCreated(purposeId, _)      => purposeId.toFutureUUID.flatMap(createMessageId(dynamoService))
    case PurposeVersionActivated(purpose)         => createMessageId(dynamoService)(purpose.id)
    case PurposeVersionSuspended(purpose)         => createMessageId(dynamoService)(purpose.id)
    case PurposeVersionWaitedForApproval(purpose) => createMessageId(dynamoService)(purpose.id)
    case PurposeVersionArchived(purpose)          => createMessageId(dynamoService)(purpose.id)
    case PurposeVersionUpdated(purposeId, _)      => purposeId.toFutureUUID.flatMap(createMessageId(dynamoService))
    case PurposeVersionDeleted(purposeId, _)      => purposeId.toFutureUUID.flatMap(createMessageId(dynamoService))
    case PurposeDeleted(purposeId)                => purposeId.toFutureUUID.flatMap(createMessageId(dynamoService))
  }

  def asDynamoPayload: PartialFunction[ProjectableEvent, Either[ComponentError, DynamoEventPayload]] = {
    case e: Event => getEventPayload(e)
  }

  private[this] def getEventPayload(event: Event): Either[ComponentError, DynamoEventPayload] = event match {
    case PurposeCreated(purpose)                  => Right(PurposeEventPayload(purpose.id.toString, CREATED.toString))
    case PurposeUpdated(purpose)                  => Right(PurposeEventPayload(purpose.id.toString, UPDATED.toString))
    case PurposeVersionCreated(purposeId, _)      => Right(PurposeEventPayload(purposeId, CREATED.toString))
    case PurposeVersionActivated(purpose)         => Right(PurposeEventPayload(purpose.id.toString, ACTIVATED.toString))
    case PurposeVersionSuspended(purpose)         => Right(PurposeEventPayload(purpose.id.toString, SUSPENDED.toString))
    case PurposeVersionWaitedForApproval(purpose) =>
      Right(PurposeEventPayload(purpose.id.toString, WAITING_FOR_APPROVAL.toString))
    case PurposeVersionArchived(purpose)          => Right(PurposeEventPayload(purpose.id.toString, ARCHIVED.toString))
    case PurposeVersionUpdated(purposeId, _)      => Right(PurposeEventPayload(purposeId, UPDATED.toString))
    case PurposeVersionDeleted(purposeId, _)      => Right(PurposeEventPayload(purposeId, DELETED.toString))
    case PurposeDeleted(purposeId)                => Right(PurposeEventPayload(purposeId, DELETED.toString))
  }
}
