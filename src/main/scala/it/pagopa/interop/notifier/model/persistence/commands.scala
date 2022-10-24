package it.pagopa.interop.notifier.model.persistence

import akka.actor.typed.ActorRef

sealed trait Command

final case class UpdateOrganizationNotificationEventId(
  organizationId: String,
  replyTo: ActorRef[Option[PersistentOrganizationEvent]]
) extends Command

case object Idle extends Command
