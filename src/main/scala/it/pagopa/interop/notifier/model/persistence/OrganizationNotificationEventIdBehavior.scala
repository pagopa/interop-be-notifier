package it.pagopa.interop.notifier.model.persistence

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityTypeKey}
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}
import it.pagopa.interop.notifier.error.NotifierErrors.OrganizationNotFound
import it.pagopa.interop.notifier.model.persistence.organization.PersistentOrganizationEvent

import java.time.temporal.ChronoUnit
import scala.concurrent.duration.{DurationInt, DurationLong}
import scala.language.postfixOps

object OrganizationNotificationEventIdBehavior {

  def commandHandler(
    shard: ActorRef[ClusterSharding.ShardCommand],
    context: ActorContext[OrganizationNotificationCommand]
  ): (
    OrganizationNotificationEventIdState,
    OrganizationNotificationCommand
  ) => Effect[OrganizationNotificationIdEvent, OrganizationNotificationEventIdState] = { (state, command) =>
    val idleTimeout = context.system.settings.config.getDuration("notifier.idle-timeout")
    context.setReceiveTimeout(idleTimeout.get(ChronoUnit.SECONDS) seconds, NotificationEventIdle)
    command match {
      case UpdateOrganizationNotificationEventId(organizationId, replyTo) =>
        val nextId: Long = state.identifiers.get(organizationId.toString).getOrElse(0L) + 1L

        Effect
          .persist(EventIdAdded(organizationId.toString, nextId))
          .thenRun((_: OrganizationNotificationEventIdState) =>
            replyTo ! StatusReply.Success(PersistentOrganizationEvent(organizationId, nextId))
          )

      case GetOrganizationNotificationEventId(organizationId, replyTo) =>
        state.identifiers.get(organizationId.toString) match {
          case Some(currentId) =>
            replyTo ! StatusReply.Success(PersistentOrganizationEvent(organizationId, currentId))
            Effect.none[OrganizationNotificationIdEvent, OrganizationNotificationEventIdState]
          case None            => commandError(replyTo, OrganizationNotFound(organizationId.toString))
        }

      case NotificationEventIdle =>
        shard ! ClusterSharding.Passivate(context.self)
        context.log.info(s"Passivate shard: ${shard.path.name}")
        Effect.none[OrganizationNotificationIdEvent, OrganizationNotificationEventIdState]
    }
  }

  private def commandError[T](
    replyTo: ActorRef[StatusReply[T]],
    error: Throwable
  ): Effect[OrganizationNotificationIdEvent, OrganizationNotificationEventIdState] = {
    replyTo ! StatusReply.Error[T](error)
    Effect.none[OrganizationNotificationIdEvent, OrganizationNotificationEventIdState]
  }

  val eventHandler
    : (OrganizationNotificationEventIdState, OrganizationNotificationIdEvent) => OrganizationNotificationEventIdState =
    (state, event) =>
      event match {
        case EventIdAdded(client, nextId) => state.increaseEventId(client, nextId)
      }

  val TypeKey: EntityTypeKey[OrganizationNotificationCommand] =
    EntityTypeKey[OrganizationNotificationCommand]("interop-be-notifier-notification-id-persistence")

  def apply(
    shard: ActorRef[ClusterSharding.ShardCommand],
    persistenceId: PersistenceId
  ): Behavior[OrganizationNotificationCommand] = {
    Behaviors.setup { context =>
      context.log.info(s"Starting Key Shard ${persistenceId.id}")
      val numberOfEvents =
        context.system.settings.config.getInt("notifier.number-of-events-before-snapshot")
      EventSourcedBehavior[
        OrganizationNotificationCommand,
        OrganizationNotificationIdEvent,
        OrganizationNotificationEventIdState
      ](
        persistenceId = persistenceId,
        emptyState = OrganizationNotificationEventIdState.empty,
        commandHandler = commandHandler(shard, context),
        eventHandler = eventHandler
      ).withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = numberOfEvents, keepNSnapshots = 1))
        .withTagger(_ => Set(persistenceId.id))
        .onPersistFailure(SupervisorStrategy.restartWithBackoff(200 millis, 5 seconds, 0.1))
    }
  }
}