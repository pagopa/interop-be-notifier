package it.pagopa.interop.notifier.service

import java.util.UUID
import scala.concurrent.Future

trait CatalogManagementService {
  def getEServiceProducerByEServiceId(contexts: Seq[(String, String)])(eServiceId: UUID): Future[UUID]
}
