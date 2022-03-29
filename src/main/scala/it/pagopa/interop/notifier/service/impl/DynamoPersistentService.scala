package it.pagopa.interop.notifier.service.impl

import it.pagopa.interop.notifier.model.{Organization, OrganizationUpdatePayload}
import it.pagopa.interop.notifier.service.PersistenceService

import java.util.UUID
import scala.concurrent.Future

class DynamoPersistentService extends PersistenceService {
  override def addOrganization(organization: Organization): Future[Boolean]                                       = ???
  override def updateOrganization(organizationId: UUID, organization: OrganizationUpdatePayload): Future[Boolean] = ???
  override def deleteOrganization(organizationId: UUID): Future[Boolean]                                          = ???
  override def getOrganization(organizationId: UUID): Future[Organization]                                        = ???
}
