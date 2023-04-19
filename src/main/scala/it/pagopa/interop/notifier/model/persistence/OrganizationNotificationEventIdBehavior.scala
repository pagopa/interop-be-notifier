package it.pagopa.interop.notifier.model.persistence

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}

import java.time.temporal.ChronoUnit
import scala.concurrent.duration.{DurationInt, DurationLong}
import scala.language.postfixOps

object OrganizationNotificationEventIdBehavior {

  def commandHandler(
    shard: ActorRef[ClusterSharding.ShardCommand],
    context: ActorContext[Command]
  ): (State, Command) => Effect[Event, State] = { (state, command) =>
    val idleTimeout = context.system.settings.config.getDuration("notifier.idle-timeout")
    context.setReceiveTimeout(idleTimeout.get(ChronoUnit.SECONDS) seconds, Idle)
    command match {
      case UpdateOrganizationNotificationEventId(organizationId, replyTo) =>
        val nextId: Long = state.identifiers.getOrElse(organizationId, 0L) + 1L

        Effect
          .persist(EventIdAdded(organizationId, nextId))
          .thenRun((_: State) => replyTo ! Some(PersistentOrganizationEvent(organizationId, nextId)))

      case Idle =>
        shard ! ClusterSharding.Passivate(context.self)
        context.log.info(s"Passivate shard: ${shard.path.name}")
        Effect.none[Event, State]
    }
  }

  val eventHandler: (State, Event) => State = (state, event) =>
    event match {
      case EventIdAdded(client, nextId) => state.increaseEventId(client, nextId)
    }

  val TypeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("interop-be-notifier-notification-id-persistence")

  def apply(
    shard: ActorRef[ClusterSharding.ShardCommand],
    persistenceId: PersistenceId,
    projectionTag: String
  ): Behavior[Command] = Behaviors.setup { context =>
    context.log.info(s"Starting Key Shard ${persistenceId.id}")
    val numberOfEvents = context.system.settings.config.getInt("notifier.number-of-events-before-snapshot")
    EventSourcedBehavior[Command, Event, State](
      persistenceId = persistenceId,
      emptyState = State.empty,
      commandHandler = commandHandler(shard, context),
      eventHandler = eventHandler
    ).withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = numberOfEvents, keepNSnapshots = 1))
      .withTagger(_ => Set(projectionTag))
      .onPersistFailure(SupervisorStrategy.restartWithBackoff(200 millis, 5 seconds, 0.1))
  }
}
