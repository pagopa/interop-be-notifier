package it.pagopa.interop.notifier.service

import it.pagopa.interop.purposemanagement.client.model.Purpose

import scala.concurrent.Future

trait PurposeManagementService {

  def getPurpose(id: String)(implicit contexts: Seq[(String, String)]): Future[Purpose]
}
