package it.pagopa.interop.notifier.service

import it.pagopa.interop.notifier.model.{Organization, OrganizationUpdatePayload}

import java.util.UUID
import scala.concurrent.Future

trait PersistenceService {
  def addOrganization(organization: Organization): Future[Unit]
  def updateOrganization(organizationId: UUID, organization: OrganizationUpdatePayload): Future[Unit]
  def deleteOrganization(organizationId: UUID): Future[Unit]
  def getOrganization(organizationId: UUID): Future[Organization]
}
