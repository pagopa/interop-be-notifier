package it.pagopa.interop.notifier.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.onComplete
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.Logger
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.TypeConversions.StringOps
import it.pagopa.interop.notifier.api.RegistryApiService
import it.pagopa.interop.notifier.error.NotifierErrors.{
  OrganizationCreationFailed,
  OrganizationDeletionFailed,
  OrganizationNotFound,
  OrganizationUpdateFailed
}
import it.pagopa.interop.notifier.model.{Organization, OrganizationUpdatePayload, Problem}
import it.pagopa.interop.notifier.service.PersistenceService
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class RegistryServiceApiImpl(persistenceService: PersistenceService)(implicit ec: ExecutionContext)
    extends RegistryApiService {

  private val logger = Logger.takingImplicit[ContextFieldsToLog](LoggerFactory.getLogger(this.getClass))

  /**
    * Code: 201, Message: Organization created
    * Code: 401, Message: Unauthorized, DataType: Problem
    */
  override def addOrganization(
    organization: Organization
  )(implicit contexts: Seq[(String, String)], toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route = {
    logger.info("Creating Organization {}", organization)

    onComplete(persistenceService.addOrganization(organization)) {
      case Success(_) =>
        addOrganization201
      case _          =>
        logger.error(s"Persistence error while creating organization")
        val problem = problemOf(StatusCodes.BadRequest, OrganizationCreationFailed)
        addOrganization400(problem)
    }
  }

  /**
    * Code: 204, Message: Organization deleted
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Client not found, DataType: Problem
    */
  override def deleteOrganization(
    organizationId: String
  )(implicit contexts: Seq[(String, String)], toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route = {
    val result: Future[Unit] = for {
      organizationUUID <- organizationId.toFutureUUID
      result           <- persistenceService.deleteOrganization(organizationUUID)
    } yield result

    onComplete(result) {
      case Success(_)  =>
        deleteOrganization204
      case Failure(ex) =>
        logger.error(s"Error deleting organization $organizationId  - ${ex.getMessage}")
        val problem = problemOf(StatusCodes.BadRequest, OrganizationDeletionFailed(organizationId))
        deleteOrganization400(problem)
    }
  }

  /**
    * Code: 200, Message: Organization retrieved, DataType: Organization
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Organization not found, DataType: Problem
    */
  override def getOrganization(organizationId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerOrganization: ToEntityMarshaller[Organization],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    val result: Future[Organization] = for {
      organizationUUID <- organizationId.toFutureUUID
      result           <- persistenceService.getOrganization(organizationUUID)
    } yield result
    onComplete(result) {
      case Success(organization) =>
        getOrganization200(organization)
      case Failure(ex)           =>
        logger.error(s"Error getting organization $organizationId  - ${ex.getMessage}")
        val problem = problemOf(StatusCodes.BadRequest, OrganizationNotFound(organizationId))
        getOrganization404(problem)
    }
  }

  /**
    * Code: 204, Message: Organization updated
    * Code: 401, Message: Unauthorized, DataType: Problem
    */
  override def updateOrganization(organizationId: String, organizationUpdatePayload: OrganizationUpdatePayload)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    val result: Future[Unit] = for {
      organizationUUID <- organizationId.toFutureUUID
      result           <- persistenceService.updateOrganization(organizationUUID, organizationUpdatePayload)
    } yield result

    onComplete(result) {
      case Success(_)  =>
        updateOrganization204
      case Failure(ex) =>
        logger.error(s"Error updating organization $organizationId  - ${ex.getMessage}")
        val problem = problemOf(StatusCodes.BadRequest, OrganizationUpdateFailed(organizationId))
        updateOrganization400(problem)
    }
  }
}
