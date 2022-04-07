package it.pagopa.interop.notifier.model.persistence.serializer

import cats.implicits.toTraverseOps
import it.pagopa.interop.notifier.model.persistence.organization.PersistentOrganization
import it.pagopa.interop.notifier.model.persistence.serializer.v1.events.{
  EventIdAddedV1,
  OrganizationAddedV1,
  OrganizationDeletedV1,
  OrganizationUpdatedV1
}
import it.pagopa.interop.notifier.model.persistence.serializer.v1.protobufUtils._
import it.pagopa.interop.notifier.model.persistence.serializer.v1.state.{
  OrganizationNotificationEventIdStateEntryV1,
  OrganizationNotificationEventIdStateV1,
  OrganizationsStateEntryV1,
  OrganizationsStateV1
}
import it.pagopa.interop.notifier.model.persistence._

package object v1 {

  type ErrorOr[A] = Either[Throwable, A]

  implicit def organizationsStateV1PersistEventDeserializer
    : PersistEventDeserializer[OrganizationsStateV1, OrganizationsState] =
    state => {
      for {
        attributes <- state.organizations
          .traverse[ErrorOr, (String, PersistentOrganization)] { entry =>
            toPersistentOrganization(entry.value).map(persistentOrganization => (entry.key, persistentOrganization))
          }
          .map(_.toMap)
      } yield OrganizationsState(attributes)
    }

  implicit def organizationsStateV1PersistEventSerializer
    : PersistEventSerializer[OrganizationsState, OrganizationsStateV1] =
    state => {
      val entries = state.organizations.toSeq.map { case (k, v) =>
        OrganizationsStateEntryV1(k, toProtobufOrganization(v))
      }
      Right[Throwable, OrganizationsStateV1](OrganizationsStateV1(entries))
    }

  implicit def organizationNotificationStateV1PersistEventDeserializer
    : PersistEventDeserializer[OrganizationNotificationEventIdStateV1, OrganizationNotificationEventIdState] =
    state => {
      Right[Throwable, OrganizationNotificationEventIdState](
        OrganizationNotificationEventIdState(state.identifiers.map(entry => entry.key -> entry.value).toMap)
      )
    }

  implicit def organizationNotificationStateV1PersistEventSerializer
    : PersistEventSerializer[OrganizationNotificationEventIdState, OrganizationNotificationEventIdStateV1] =
    state => {
      val entries = state.identifiers.toSeq.map { case (k, v) =>
        OrganizationNotificationEventIdStateEntryV1(k, v)
      }
      Right[Throwable, OrganizationNotificationEventIdStateV1](OrganizationNotificationEventIdStateV1(entries))
    }

  implicit def organizationAddedV1PersistEventDeserializer
    : PersistEventDeserializer[OrganizationAddedV1, OrganizationAdded] =
    event => toPersistentOrganization(event.organization).map(OrganizationAdded)

  implicit def organizationAddedV1PersistEventSerializer
    : PersistEventSerializer[OrganizationAdded, OrganizationAddedV1] =
    event => Right[Throwable, OrganizationAddedV1](OrganizationAddedV1(toProtobufOrganization(event.organization)))

  implicit def organizationUpdatedV1PersistEventDeserializer
    : PersistEventDeserializer[OrganizationUpdatedV1, OrganizationUpdated] =
    event => toPersistentOrganization(event.organization).map(OrganizationUpdated)

  implicit def organizationUpdatedV1PersistEventSerializer
    : PersistEventSerializer[OrganizationUpdated, OrganizationUpdatedV1] =
    event => Right[Throwable, OrganizationUpdatedV1](OrganizationUpdatedV1(toProtobufOrganization(event.organization)))

  implicit def organizationDeletedV1PersistEventDeserializer
    : PersistEventDeserializer[OrganizationDeletedV1, OrganizationDeleted] =
    event => Right[Throwable, OrganizationDeleted](OrganizationDeleted(event.organizationId))

  implicit def organizationDeletedV1PersistEventSerializer
    : PersistEventSerializer[OrganizationDeleted, OrganizationDeletedV1] =
    event => Right[Throwable, OrganizationDeletedV1](OrganizationDeletedV1(event.organizationId))

  implicit def eventIdAddedV1PersistEventDeserializer: PersistEventDeserializer[EventIdAddedV1, EventIdAdded] =
    event => Right[Throwable, EventIdAdded](EventIdAdded(event.organizationId, event.id))

  implicit def eventIdAddedV1PersistEventSerializer: PersistEventSerializer[EventIdAdded, EventIdAddedV1] =
    event => Right[Throwable, EventIdAddedV1](EventIdAddedV1(event.organizationId, event.id))

}
