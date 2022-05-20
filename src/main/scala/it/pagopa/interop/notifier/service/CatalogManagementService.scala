package it.pagopa.interop.notifier.service

import java.util.UUID
import scala.concurrent.Future

trait CatalogManagementService {
  def getEServiceProducerByEServiceId(eServiceId: UUID)(implicit contexts: Seq[(String, String)]): Future[UUID]
}
