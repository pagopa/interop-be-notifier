package it.pagopa.interop.notifier.api.impl

import akka.Done
import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityRef}
import akka.cluster.sharding.typed.{ClusterShardingSettings, ShardingEnvelope}
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.onComplete
import akka.http.scaladsl.server.Route
import akka.pattern.StatusReply
import com.typesafe.scalalogging.Logger
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.TypeConversions.StringOps
import it.pagopa.interop.notifier.api.RegistryApiService
import it.pagopa.interop.notifier.common.system.timeout
import it.pagopa.interop.notifier.error.NotifierErrors.{
  OrganizationCreationFailed,
  OrganizationDeletionFailed,
  OrganizationNotFound,
  OrganizationUpdateFailed
}
import it.pagopa.interop.notifier.model.persistence._
import it.pagopa.interop.notifier.model.{Organization, OrganizationUpdatePayload, Problem}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class RegistryServiceApiImpl(
  system: ActorSystem[_],
  sharding: ClusterSharding,
  entity: Entity[OrganizationCommand, ShardingEnvelope[OrganizationCommand]]
)(implicit ec: ExecutionContext)
    extends RegistryApiService {

  private val logger = Logger.takingImplicit[ContextFieldsToLog](LoggerFactory.getLogger(this.getClass))

  private val settings: ClusterShardingSettings = entity.settings match {
    case None    => ClusterShardingSettings(system)
    case Some(s) => s
  }

  @inline private def getShard(id: String): String = (math.abs(id.hashCode) % settings.numberOfShards).toString

  /**
    * Code: 201, Message: Organization created
    * Code: 401, Message: Unauthorized, DataType: Problem
    */
  override def addOrganization(
    organization: Organization
  )(implicit contexts: Seq[(String, String)], toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route = {
    logger.info("Creating Organization {}", organization)

    val commander: EntityRef[OrganizationCommand] =
      sharding.entityRefFor(OrganizationPersistentBehavior.TypeKey, getShard(organization.organizationId.toString))
    val result: Future[StatusReply[Organization]] = commander.ask(ref => AddOrganization(organization, ref))

    onComplete(result) {
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

    val commander: EntityRef[OrganizationCommand] =
      sharding.entityRefFor(OrganizationPersistentBehavior.TypeKey, getShard(organizationId))
    val result: Future[StatusReply[Done]]         = commander.ask(ref => DeleteOrganization(organizationId, ref))

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
    val commander: EntityRef[OrganizationCommand] =
      sharding.entityRefFor(OrganizationPersistentBehavior.TypeKey, getShard(organizationId))
    val result: Future[StatusReply[Organization]] = commander.ask(ref => GetOrganization(organizationId, ref))

    onComplete(result) {
      case Success(organization) =>
        getOrganization200(organization.getValue)
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
    val commander: EntityRef[OrganizationCommand] =
      sharding.entityRefFor(OrganizationPersistentBehavior.TypeKey, getShard(organizationId))

    val result = for {
      uuid   <- organizationId.toFutureUUID
      result <- commander.ask(ref => UpdateOrganization(uuid, organizationUpdatePayload, ref))
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
