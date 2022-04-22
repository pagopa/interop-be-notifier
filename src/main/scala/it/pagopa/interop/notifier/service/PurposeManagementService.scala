package it.pagopa.interop.notifier.service

import it.pagopa.interop.purposemanagement.client.model.{Purpose, PurposeVersionState, Purposes}

import java.util.UUID
import scala.concurrent.Future

trait PurposeManagementService {

  def getPurpose(contexts: Seq[(String, String)])(id: String): Future[Purpose]
  def getPurposes(
    contexts: Seq[(String, String)]
  )(eserviceId: Option[UUID], consumerId: Option[UUID], states: Seq[PurposeVersionState]): Future[Purposes]
}
