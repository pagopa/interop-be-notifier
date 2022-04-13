package it.pagopa.interop.notifier.service.impl

import it.pagopa.interop.notifier.model.Message

import scala.concurrent.Future

class QueueHandler(val idRetriever: EventIdRetriever) {
  def processMessage(msg: Message): Future[Unit] = {

    /* for {
      organizationId <- getRecipientOrganizationId(msg).toFutureUUID
      nextEvent      <- idRetriever.getNextEventIdForOrganization(organizationId)
      messageForDynamo = serializeMessageForPersistence(msg, nextEvent)
      _                = persistOnDynamo(messageForDynamo)
    } yield () */
    ???
  }
}
