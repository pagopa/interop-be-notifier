package it.pagopa.interop.notifier.service

import it.pagopa.interop.agreementmanagement.client.model._

import scala.concurrent.Future

trait AgreementManagementService {

  def getAgreementById(contexts: Seq[(String, String)])(agreementId: String): Future[Agreement]

  def getAgreements(contexts: Seq[(String, String)])(
    producerId: Option[String] = None,
    consumerId: Option[String] = None,
    eserviceId: Option[String] = None,
    descriptorId: Option[String] = None,
    state: Option[AgreementState] = None
  ): Future[Seq[Agreement]]
}
