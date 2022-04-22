package it.pagopa.interop.notifier.service.converters

import it.pagopa.interop.commons.queue.message.ProjectableEvent
import it.pagopa.interop.commons.utils.errors.ComponentError
import it.pagopa.interop.notifier.model.{DynamoEventPayload, PurposeEventPayload}
import it.pagopa.interop.notifier.service.{CatalogManagementService, PurposeManagementService}
import it.pagopa.interop.purposemanagement.model.persistence._

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

object PurposeEventsConverter {
  def getRecipient(
    catalogManagementService: CatalogManagementService,
    purposeManagementService: PurposeManagementService,
    contexts: Seq[(String, String)]
  )(implicit ec: ExecutionContext): PartialFunction[ProjectableEvent, Future[UUID]] = {
    def producer(purposeId: String): Future[UUID] = for {
      p        <- purposeManagementService.getPurpose(contexts)(purposeId)
      producer <- catalogManagementService.getEServiceProducerByEServiceId(contexts)(p.eserviceId)
    } yield producer

    val getter: PartialFunction[ProjectableEvent, Future[UUID]] = {
      case PurposeCreated(purpose)                  =>
        catalogManagementService.getEServiceProducerByEServiceId(contexts)(purpose.eserviceId)
      case PurposeUpdated(purpose)                  =>
        catalogManagementService.getEServiceProducerByEServiceId(contexts)(purpose.eserviceId)
      case PurposeVersionCreated(purposeId, _)      => producer(purposeId)
      case PurposeVersionActivated(purpose)         =>
        catalogManagementService.getEServiceProducerByEServiceId(contexts)(purpose.eserviceId)
      case PurposeVersionSuspended(purpose)         =>
        catalogManagementService.getEServiceProducerByEServiceId(contexts)(purpose.eserviceId)
      case PurposeVersionWaitedForApproval(purpose) =>
        catalogManagementService.getEServiceProducerByEServiceId(contexts)(purpose.eserviceId)
      case PurposeVersionArchived(purpose)          =>
        catalogManagementService.getEServiceProducerByEServiceId(contexts)(purpose.eserviceId)
      case PurposeVersionUpdated(purposeId, _)      => producer(purposeId)
      case PurposeVersionDeleted(purposeId, _)      => producer(purposeId)
      case PurposeDeleted(purposeId)                => producer(purposeId)
    }
    getter
  }

  def asDynamoPayload: PartialFunction[ProjectableEvent, Either[ComponentError, DynamoEventPayload]] = {
    case PurposeCreated(purpose)                  => Right(PurposeEventPayload(purpose.id.toString, CREATED))
    case PurposeUpdated(purpose)                  => Right(PurposeEventPayload(purpose.id.toString, UPDATED))
    case PurposeVersionCreated(purposeId, _)      => Right(PurposeEventPayload(purposeId, CREATED))
    case PurposeVersionActivated(purpose)         => Right(PurposeEventPayload(purpose.id.toString, ACTIVATED))
    case PurposeVersionSuspended(purpose)         => Right(PurposeEventPayload(purpose.id.toString, SUSPENDED))
    case PurposeVersionWaitedForApproval(purpose) =>
      Right(PurposeEventPayload(purpose.id.toString, WAITING_FOR_APPROVAL))
    case PurposeVersionArchived(purpose)          => Right(PurposeEventPayload(purpose.id.toString, ARCHIVED))
    case PurposeVersionUpdated(purposeId, _)      => Right(PurposeEventPayload(purposeId, UPDATED))
    case PurposeVersionDeleted(purposeId, _)      => Right(PurposeEventPayload(purposeId, DELETED))
    case PurposeDeleted(purposeId)                => Right(PurposeEventPayload(purposeId, DELETED))
  }
}
