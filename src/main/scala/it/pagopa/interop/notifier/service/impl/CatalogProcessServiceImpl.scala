package it.pagopa.interop.notifier.service.impl

import it.pagopa.interop.catalogprocess.client.invoker.BearerToken
import it.pagopa.interop.catalogprocess.client.model.EService
import it.pagopa.interop.commons.utils.withHeaders
import it.pagopa.interop.notifier.service.{CatalogProcessApi, CatalogProcessInvoker, CatalogProcessService}
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

final class CatalogProcessServiceImpl(invoker: CatalogProcessInvoker, api: CatalogProcessApi)(implicit
  ec: ExecutionContext
) extends CatalogProcessService {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  private[this] def getEServiceById(eServiceId: UUID)(implicit contexts: Seq[(String, String)]): Future[EService] =
    withHeaders { (bearerToken, correlationId) =>
      val request = api.getEServiceById(xCorrelationId = correlationId, eServiceId)(BearerToken(bearerToken))
      invoker.invoke(request, s"Retrieving EService $eServiceId")
    }

  override def getEServiceProducerByEServiceId(eServiceId: UUID)(implicit
    contexts: Seq[(String, String)]
  ): Future[UUID] = getEServiceById(eServiceId).map(_.producerId)
}
