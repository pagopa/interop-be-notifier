package it.pagopa.interop.notifier.service

import it.pagopa.interop.authorizationmanagement.model.persistence.{KeyDeleted, KeysAdded}
import it.pagopa.interop.commons.queue.message.Message
import it.pagopa.interop.notifier.database.AuthorizationEventsDao._
import it.pagopa.interop.notifier.service.converters.EventType

import scala.concurrent.{ExecutionContext, Future}

final class AuthorizationEventsHandler(blockingEc: ExecutionContext) {

  def handleEvents: PartialFunction[Message, Future[Unit]] = { case m: Message =>
    m.payload match {
      case KeysAdded(_, keys)    => insertKeysNotifications(keys.keys.toList, EventType.ADDED).map(_ => ())(blockingEc)
      case KeyDeleted(_, kid, _) => insertKeyNotification(kid, EventType.DELETED).map(_ => ())(blockingEc)
      case _                     => Future.unit
    }
  }

}
