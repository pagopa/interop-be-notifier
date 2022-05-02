package it.pagopa.interop.notifier.service

import it.pagopa.interop.purposemanagement.client.model.Purpose

import scala.concurrent.Future

trait PurposeManagementService {

  def getPurpose(contexts: Seq[(String, String)])(id: String): Future[Purpose]

}
