package it.pagopa.interop.notifier.service.converters

import it.pagopa.interop.commons.queue.message.ProjectableEvent
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.commons.utils.errors.ComponentError
import it.pagopa.interop.notifier.model.{DynamoEventPayload, PurposeEventPayload}
import it.pagopa.interop.notifier.service.converters.EventType._
import it.pagopa.interop.notifier.service.{CatalogManagementService, DynamoService}
import it.pagopa.interop.purposemanagement.model.persistence._

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

object PurposeEventsConverter {
  def getRecipient(catalogManagementService: CatalogManagementService, dynamoService: DynamoService)(implicit
    ec: ExecutionContext,
    contexts: Seq[(String, String)]
  ): PartialFunction[ProjectableEvent, Future[UUID]] = { case e: Event =>
    getEventRecipient(catalogManagementService, dynamoService, e)
  }

  private[this] def getEventRecipient(
    catalogManagementService: CatalogManagementService,
    dynamoService: DynamoService,
    event: Event
  )(implicit ec: ExecutionContext, contexts: Seq[(String, String)]): Future[UUID] = {

    event match {
      case PurposeCreated(purpose)                  =>
        catalogManagementService.getEServiceProducerByEServiceId(purpose.eserviceId)
      case PurposeUpdated(purpose)                  => dynamoService.getOrganizationId(purpose.id)
      case PurposeVersionCreated(purposeId, _)      => purposeId.toFutureUUID.flatMap(dynamoService.getOrganizationId)
      case PurposeVersionActivated(purpose)         => dynamoService.getOrganizationId(purpose.id)
      case PurposeVersionSuspended(purpose)         => dynamoService.getOrganizationId(purpose.id)
      case PurposeVersionWaitedForApproval(purpose) => dynamoService.getOrganizationId(purpose.id)
      case PurposeVersionArchived(purpose)          => dynamoService.getOrganizationId(purpose.id)
      case PurposeVersionUpdated(purposeId, _)      => purposeId.toFutureUUID.flatMap(dynamoService.getOrganizationId)
      case PurposeVersionDeleted(purposeId, _)      => purposeId.toFutureUUID.flatMap(dynamoService.getOrganizationId)
      case PurposeDeleted(purposeId)                => purposeId.toFutureUUID.flatMap(dynamoService.getOrganizationId)
    }
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
