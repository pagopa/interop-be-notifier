package it.pagopa.interop.notifier.service.converters

import it.pagopa.interop.commons.queue.message.ProjectableEvent
import it.pagopa.interop.commons.utils.errors.ComponentError
import it.pagopa.interop.notifier.model.{DynamoEventPayload, PurposeEventPayload}
import it.pagopa.interop.notifier.service.{CatalogManagementService, PurposeManagementService}
import it.pagopa.interop.purposemanagement.model.persistence._
import it.pagopa.interop.notifier.service.converters.EventType._

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

object PurposeEventsConverter {
  def getRecipient(
    catalogManagementService: CatalogManagementService,
    purposeManagementService: PurposeManagementService,
    contexts: Seq[(String, String)]
  )(implicit ec: ExecutionContext): PartialFunction[ProjectableEvent, Future[UUID]] = { case e: Event =>
    getEventRecipient(catalogManagementService, purposeManagementService, contexts, e)
  }

  private[this] def getEventRecipient(
    catalogManagementService: CatalogManagementService,
    purposeManagementService: PurposeManagementService,
    contexts: Seq[(String, String)],
    event: Event
  )(implicit ec: ExecutionContext): Future[UUID] = {
    def producer(purposeId: String): Future[UUID] = for {
      p        <- purposeManagementService.getPurpose(contexts)(purposeId)
      producer <- catalogManagementService.getEServiceProducerByEServiceId(contexts)(p.eserviceId)
    } yield producer

    event match {
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
