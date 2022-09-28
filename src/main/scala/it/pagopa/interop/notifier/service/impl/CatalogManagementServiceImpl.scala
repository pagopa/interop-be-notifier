package it.pagopa.interop.notifier.service.impl

import it.pagopa.interop.catalogmanagement.client.invoker.BearerToken
import it.pagopa.interop.catalogmanagement.client.model.EService
import it.pagopa.interop.commons.utils.withHeaders
import it.pagopa.interop.notifier.service.{CatalogManagementApi, CatalogManagementInvoker, CatalogManagementService}
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

final class CatalogManagementServiceImpl(invoker: CatalogManagementInvoker, api: CatalogManagementApi)(implicit
  ec: ExecutionContext
) extends CatalogManagementService {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  private[this] def getEServiceById(eServiceId: UUID)(implicit contexts: Seq[(String, String)]): Future[EService] =
    withHeaders { (bearerToken, correlationId, ip) =>
      val request = api.getEService(xCorrelationId = correlationId, eServiceId.toString, xForwardedFor = ip)(
        BearerToken(bearerToken)
      )
      invoker.invoke(request, s"Retrieving EService $eServiceId")
    }

  override def getEServiceProducerByEServiceId(eServiceId: UUID)(implicit
    contexts: Seq[(String, String)]
  ): Future[UUID] = getEServiceById(eServiceId).map(_.producerId)
}
