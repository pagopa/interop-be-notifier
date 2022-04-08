package it.pagopa.interop.notifier.model.persistence

import akka.actor.typed.ActorRef
import akka.pattern.StatusReply

import java.util.UUID

sealed trait Command

final case class GetOrganizationNotificationEventId(
  organizationId: UUID,
  replyTo: ActorRef[StatusReply[PersistentOrganizationEvent]]
) extends Command
final case class UpdateOrganizationNotificationEventId(
  organizationId: UUID,
  replyTo: ActorRef[StatusReply[PersistentOrganizationEvent]]
) extends Command

case object Idle extends Command
