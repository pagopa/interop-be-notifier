package it.pagopa.interop.notifier.service.impl

import it.pagopa.interop.commons.utils.TypeConversions.EitherOps
import it.pagopa.interop.commons.utils.TypeConversions.StringOps
import it.pagopa.interop.commons.utils.extractHeaders
import it.pagopa.interop.notifier.service.{PurposeManagementApi, PurposeManagementInvoker, PurposeManagementService}
import it.pagopa.interop.purposemanagement.client.invoker.BearerToken
import it.pagopa.interop.purposemanagement.client.model.{Purpose, PurposeVersionState, Purposes}
import org.slf4j.{Logger, LoggerFactory}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

final case class PurposeManagementServiceImpl(invoker: PurposeManagementInvoker, api: PurposeManagementApi)(implicit
  ec: ExecutionContext
) extends PurposeManagementService {

  implicit val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def getPurpose(contexts: Seq[(String, String)])(id: String): Future[Purpose] = {
    for {
      id                               <- id.toFutureUUID
      (bearerToken, correlationId, ip) <- extractHeaders(contexts).toFuture
      request = api.getPurpose(xCorrelationId = correlationId, id, xForwardedFor = ip)(BearerToken(bearerToken))
      result <- invoker.invoke(request, s"Retrieving purpose $id")
    } yield result

  }

  override def getPurposes(
    contexts: Seq[(String, String)]
  )(eserviceId: Option[UUID], consumerId: Option[UUID], states: Seq[PurposeVersionState]): Future[Purposes] = {
    for {
      (bearerToken, correlationId, ip) <- extractHeaders(contexts).toFuture
      request = api.getPurposes(xCorrelationId = correlationId, xForwardedFor = ip, eserviceId, consumerId, states)(
        BearerToken(bearerToken)
      )
      result <- invoker.invoke(
        request,
        s"Retrieving purposes for EService $eserviceId, Consumer $consumerId and States $states"
      )
    } yield result
  }

}
