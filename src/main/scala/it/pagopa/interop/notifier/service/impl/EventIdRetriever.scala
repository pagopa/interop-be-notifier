package it.pagopa.interop.notifier.service.impl

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityRef}
import akka.cluster.sharding.typed.{ClusterShardingSettings, ShardingEnvelope}
import akka.util.Timeout
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.notifier.error.NotifierErrors.OrganizationNotFound
import it.pagopa.interop.notifier.model.persistence.{
  Command,
  OrganizationNotificationEventIdBehavior,
  PersistentOrganizationEvent,
  UpdateOrganizationNotificationEventId
}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

final class EventIdRetriever(
  system: ActorSystem[_],
  sharding: ClusterSharding,
  entity: Entity[Command, ShardingEnvelope[Command]]
)(implicit ec: ExecutionContext) {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  implicit val timeout: Timeout = Timeout(300.seconds)

  private val settings: ClusterShardingSettings = entity.settings.getOrElse(ClusterShardingSettings(system))

  @inline private def getShard(id: String): String = (math.abs(id.hashCode) % settings.numberOfShards).toString

  def getNextEventIdForOrganization(
    organizationId: String
  )(implicit contexts: Seq[(String, String)]): Future[PersistentOrganizationEvent] = {
    logger.info("Getting next id for organization {}", organizationId)

    val commander: EntityRef[Command] =
      sharding.entityRefFor(OrganizationNotificationEventIdBehavior.TypeKey, getShard(organizationId))

    commander.ask(ref => UpdateOrganizationNotificationEventId(organizationId, ref)).flatMap {
      case Some(res) => Future.successful(res)
      case None      => Future.failed(OrganizationNotFound(organizationId))
    }

  }

}
