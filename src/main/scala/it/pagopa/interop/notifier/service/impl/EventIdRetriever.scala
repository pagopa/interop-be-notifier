package it.pagopa.interop.notifier.service.impl

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityRef}
import akka.cluster.sharding.typed.{ClusterShardingSettings, ShardingEnvelope}
import it.pagopa.interop.notifier.common.system.timeout
import it.pagopa.interop.notifier.error.NotifierErrors.OrganizationNotFound
import it.pagopa.interop.notifier.model.persistence.{
  Command,
  OrganizationNotificationEventIdBehavior,
  PersistentOrganizationEvent,
  UpdateOrganizationNotificationEventId
}
import org.slf4j.LoggerFactory

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

final class EventIdRetriever(
  system: ActorSystem[_],
  sharding: ClusterSharding,
  entity: Entity[Command, ShardingEnvelope[Command]]
)(implicit ec: ExecutionContext) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  private val settings: ClusterShardingSettings = entity.settings match {
    case None    => ClusterShardingSettings(system)
    case Some(s) => s
  }

  @inline private def getShard(id: String): String = (math.abs(id.hashCode) % settings.numberOfShards).toString

  /** Code: 201, Message: Attribute created, DataType: Attribute
    * Code: 400, Message: Bad Request, DataType: Problem
    */
  def getNextEventIdForOrganization(organizationId: UUID): Future[PersistentOrganizationEvent] = {
    logger.info("Getting next id for organization {}", organizationId)

    val commander: EntityRef[Command] =
      sharding.entityRefFor(OrganizationNotificationEventIdBehavior.TypeKey, getShard(organizationId.toString))

    val evt = for {
      eventData <- commander.ask(ref => UpdateOrganizationNotificationEventId(organizationId, ref))
    } yield eventData

    evt.flatMap(x =>
      x match {
        case Some(res) => Future.successful(res)
        case None      => Future.failed[PersistentOrganizationEvent](OrganizationNotFound(organizationId.toString))
      }
    )
  }

}
