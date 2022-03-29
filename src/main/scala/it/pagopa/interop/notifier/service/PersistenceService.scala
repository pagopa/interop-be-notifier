package it.pagopa.interop.notifier.service

import it.pagopa.interop.notifier.model.{Organization, OrganizationUpdatePayload}

import java.util.UUID
import scala.concurrent.Future

trait PersistenceService {
  def addOrganization(organization: Organization): Future[Boolean]
  def updateOrganization(organizationId: UUID, organization: OrganizationUpdatePayload): Future[Boolean]
  def deleteOrganization(organizationId: UUID): Future[Boolean]
  def getOrganization(organizationId: UUID): Future[Organization]
}
