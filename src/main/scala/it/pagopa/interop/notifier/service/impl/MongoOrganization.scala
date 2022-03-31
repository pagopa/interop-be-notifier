package it.pagopa.interop.notifier.service.impl

import it.pagopa.interop.notifier.model.Organization
import org.mongodb.scala.bson.ObjectId

import java.util.UUID

case class MongoOrganization(_id: ObjectId, notificationURL: String, audience: String, organizationId: String)

object MongoOrganization {
  def toMongo(organization: Organization): MongoOrganization =
    MongoOrganization(
      new ObjectId(),
      organization.notificationURL,
      organization.audience,
      organization.organizationId.toString
    )

  def fromMongo(organization: MongoOrganization): Organization =
    Organization(organization.notificationURL, organization.audience, UUID.fromString(organization.organizationId))

  def apply(notificationURL: String, audience: String, organizationId: String): MongoOrganization =
    MongoOrganization(new ObjectId(), notificationURL, audience, organizationId)
}
