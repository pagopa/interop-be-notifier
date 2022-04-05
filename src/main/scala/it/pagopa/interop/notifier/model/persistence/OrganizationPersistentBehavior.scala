package it.pagopa.interop.notifier.model.persistence

import akka.Done
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityTypeKey}
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}
import it.pagopa.interop.notifier.error.NotifierErrors.OrganizationNotFound
import it.pagopa.interop.notifier.model.persistence.organization.PersistentOrganization
import it.pagopa.interop.notifier.model.{Organization, persistence}

import java.time.temporal.ChronoUnit
import scala.concurrent.duration.{DurationInt, DurationLong}
import scala.language.postfixOps

object OrganizationPersistentBehavior {

  def commandHandler(
    shard: ActorRef[ClusterSharding.ShardCommand],
    context: ActorContext[OrganizationCommand]
  ): (OrganizationsState, OrganizationCommand) => Effect[OrganizationEvent, OrganizationsState] = { (state, command) =>
    val idleTimeout = context.system.settings.config.getDuration("notifier.idle-timeout")
    context.setReceiveTimeout(idleTimeout.get(ChronoUnit.SECONDS) seconds, Idle)
    command match {
      case AddOrganization(organization, replyTo) =>
        val client: Option[PersistentOrganization] = state.organizations.get(organization.organizationId.toString)
        client
          .map { c =>
            replyTo ! StatusReply.Error[Organization](s"Organization ${c.organizationId.toString} already exists")
            Effect.none[OrganizationAdded, OrganizationsState]
          }
          .getOrElse {
            Effect
              .persist(persistence.OrganizationAdded(PersistentOrganization.toPersistentOrganization(organization)))
              .thenRun((_: OrganizationsState) => replyTo ! StatusReply.Success(organization))
          }

      case GetOrganization(organizationId, replyTo) =>
        state.organizations.get(organizationId) match {
          case Some(org) =>
            replyTo ! StatusReply.Success(PersistentOrganization.toAPI(org))
            Effect.none[OrganizationEvent, OrganizationsState]
          case None      => commandError(replyTo, OrganizationNotFound(organizationId))
        }

      case UpdateOrganization(organizationId, payload, replyTo) =>
        val catalogItem: Option[PersistentOrganization] = state.organizations.get(organizationId.toString)

        catalogItem
          .map { _ =>
            val updated = PersistentOrganization(
              organizationId = organizationId,
              notificationURL = payload.notificationURL,
              audience = payload.audience
            )
            Effect
              .persist(OrganizationUpdated(updated))
              .thenRun((_: OrganizationsState) => replyTo ! StatusReply.Success(PersistentOrganization.toAPI(updated)))
          }
          .getOrElse {
            replyTo ! StatusReply.Error[Organization](s"Organization ${organizationId.toString} not found")
            Effect.none[OrganizationUpdated, OrganizationsState]
          }

      case DeleteOrganization(organizationId, replyTo) =>
        val client: Option[PersistentOrganization] = state.organizations.get(organizationId)
        client
          .fold(commandError(replyTo, OrganizationNotFound(organizationId)))(_ =>
            Effect
              .persist(OrganizationDeleted(organizationId))
              .thenRun((_: OrganizationsState) => replyTo ! StatusReply.Success(Done))
          )

      case Idle =>
        shard ! ClusterSharding.Passivate(context.self)
        context.log.info(s"Passivate shard: ${shard.path.name}")
        Effect.none[OrganizationEvent, OrganizationsState]
    }
  }

  private def commandError[T](
    replyTo: ActorRef[StatusReply[T]],
    error: Throwable
  ): Effect[OrganizationEvent, OrganizationsState] = {
    replyTo ! StatusReply.Error[T](error)
    Effect.none[OrganizationEvent, OrganizationsState]
  }

  val eventHandler: (OrganizationsState, OrganizationEvent) => OrganizationsState = (state, event) =>
    event match {
      case OrganizationAdded(client)     => state.upsertOrganization(client)
      case OrganizationUpdated(client)   => state.upsertOrganization(client)
      case OrganizationDeleted(clientId) => state.deleteOrganization(clientId)
    }

  val TypeKey: EntityTypeKey[OrganizationCommand] =
    EntityTypeKey[OrganizationCommand]("interop-be-notifier-persistence")

  def apply(
    shard: ActorRef[ClusterSharding.ShardCommand],
    persistenceId: PersistenceId
  ): Behavior[OrganizationCommand] = {
    Behaviors.setup { context =>
      context.log.info(s"Starting Key Shard ${persistenceId.id}")
      val numberOfEvents =
        context.system.settings.config.getInt("notifier.number-of-events-before-snapshot")
      EventSourcedBehavior[OrganizationCommand, OrganizationEvent, OrganizationsState](
        persistenceId = persistenceId,
        emptyState = OrganizationsState.empty,
        commandHandler = commandHandler(shard, context),
        eventHandler = eventHandler
      ).withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = numberOfEvents, keepNSnapshots = 1))
        .withTagger(_ => Set(persistenceId.id))
        .onPersistFailure(SupervisorStrategy.restartWithBackoff(200 millis, 5 seconds, 0.1))
    }
  }
}
