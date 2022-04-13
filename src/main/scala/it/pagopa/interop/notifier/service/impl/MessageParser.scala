package it.pagopa.interop.notifier.service.impl

import it.pagopa.interop.notifier.model.Message
import it.pagopa.interop.notifier.model.persistence.PersistentOrganizationEvent

object MessageParser {
  def getRecipientOrganizationId(message: Message): String = ???
}

object MessageSerializer {
  def serializeMessageForPersistence(message: Message, nextEvent: PersistentOrganizationEvent) = ???
}

object DynamoPersistence {
  def persistOnDynamo(serializedMessage: String): Unit = ???
}
