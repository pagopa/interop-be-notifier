package it.pagopa.interop.notifier.service

import it.pagopa.interop.commons.queue.message.Message
import it.pagopa.interop.notifier.model.DynamoMessage

import java.util.UUID
import scala.concurrent.Future

/**
  * Exposes methods for operations on DynamoDB tables
  */
trait DynamoService {

  /**
    * Adds a new entry in the DynamoDB table holding messages
    * @param organizationId - unique identifier of the organization this message belongs to
    * @param eventId - identifier of the persisted message
    * @param message - message to be persisted
    * @return
    */
  def put(organizationId: UUID, eventId: Long, message: Message): Future[Unit]

  /**
    * Returns list of persisted messages by oranizationId and eventId
    * @param limit - max number of retrieved entries
    * @param organizationId - 
    * @param eventId
    * @return
    */
  def get(limit: Int)(organizationId: UUID, eventId: Long)(implicit
    contexts: Seq[(String, String)]
  ): Future[List[DynamoMessage]]

  def getOrganizationId(resourceId: UUID)(implicit contexts: Seq[(String, String)]): Future[UUID]
}
