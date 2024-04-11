package it.pagopa.interop.notifier.service

import java.util.UUID
import scala.concurrent.Future

trait CatalogProcessService {
  def getEServiceProducerByEServiceId(eServiceId: UUID)(implicit contexts: Seq[(String, String)]): Future[UUID]
}
