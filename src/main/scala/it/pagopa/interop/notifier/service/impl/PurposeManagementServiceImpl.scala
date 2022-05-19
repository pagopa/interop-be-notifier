package it.pagopa.interop.notifier.service.impl

import it.pagopa.interop.commons.utils.TypeConversions.{EitherOps, StringOps}
import it.pagopa.interop.commons.utils.extractHeaders
import it.pagopa.interop.notifier.service.{PurposeManagementApi, PurposeManagementInvoker, PurposeManagementService}
import it.pagopa.interop.purposemanagement.client.invoker.BearerToken
import it.pagopa.interop.purposemanagement.client.model.Purpose
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}

import scala.concurrent.{ExecutionContext, Future}

final case class PurposeManagementServiceImpl(invoker: PurposeManagementInvoker, api: PurposeManagementApi)(implicit
  ec: ExecutionContext
) extends PurposeManagementService {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  override def getPurpose(id: String)(implicit contexts: Seq[(String, String)]): Future[Purpose] = for {
    id                               <- id.toFutureUUID
    (bearerToken, correlationId, ip) <- extractHeaders(contexts).toFuture
    request = api.getPurpose(xCorrelationId = correlationId, id, xForwardedFor = ip)(BearerToken(bearerToken))
    result <- invoker.invoke(request, s"Retrieving purpose $id")
  } yield result

}
