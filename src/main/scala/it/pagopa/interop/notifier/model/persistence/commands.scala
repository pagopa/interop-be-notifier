package it.pagopa.interop.notifier.model.persistence

import akka.Done
import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import it.pagopa.interop.notifier.model.persistence.organization.PersistentOrganizationEvent
import it.pagopa.interop.notifier.model.{Organization, OrganizationUpdatePayload}

import java.util.UUID

sealed trait Command
sealed trait OrganizationCommand             extends Command
sealed trait OrganizationNotificationCommand extends Command

final case class AddOrganization(organization: Organization, replyTo: ActorRef[StatusReply[Organization]])
    extends OrganizationCommand
final case class UpdateOrganization(
  organizationId: UUID,
  organization: OrganizationUpdatePayload,
  replyTo: ActorRef[StatusReply[Organization]]
) extends OrganizationCommand
final case class GetOrganization(organizationId: String, replyTo: ActorRef[StatusReply[Organization]])
    extends OrganizationCommand
final case class DeleteOrganization(organizationId: String, replyTo: ActorRef[StatusReply[Done]])
    extends OrganizationCommand

final case class GetOrganizationNotificationEventId(
  organizationId: UUID,
  replyTo: ActorRef[StatusReply[PersistentOrganizationEvent]]
) extends OrganizationNotificationCommand
final case class UpdateOrganizationNotificationEventId(
  organizationId: UUID,
  replyTo: ActorRef[StatusReply[PersistentOrganizationEvent]]
) extends OrganizationNotificationCommand

case object Idle                  extends OrganizationCommand
case object NotificationEventIdle extends OrganizationNotificationCommand
