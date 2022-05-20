package it.pagopa.interop.notifier.service

import it.pagopa.interop.agreementmanagement.client.model._

import scala.concurrent.Future

trait AgreementManagementService {

  def getAgreementById(agreementId: String)(implicit contexts: Seq[(String, String)]): Future[Agreement]

  def getAgreements(
    producerId: Option[String] = None,
    consumerId: Option[String] = None,
    eserviceId: Option[String] = None,
    descriptorId: Option[String] = None,
    state: Option[AgreementState] = None
  )(implicit contexts: Seq[(String, String)]): Future[Seq[Agreement]]
}
