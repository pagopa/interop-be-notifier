package it.pagopa.interop.notifier.service

import it.pagopa.interop.commons.queue.message.ProjectableEvent
import it.pagopa.interop.commons.utils.errors.ComponentError
import it.pagopa.interop.notifier.error.NotifierErrors.{DynamoConverterNotFound, MessageRecipientNotFound}
import it.pagopa.interop.notifier.model.DynamoEventPayload

import java.util.UUID
import scala.concurrent.Future

package object converters {
  val ADDED                = "ADDED"
  val CREATED              = "CREATED"
  val DELETED              = "DELETED"
  val UPDATED              = "UPDATED"
  val SUSPENDED            = "SUSPENDED"
  val DEACTIVATED          = "DEACTIVATED"
  val ACTIVATED            = "ACTIVATED"
  val ARCHIVED             = "ARCHIVED"
  val WAITING_FOR_APPROVAL = "WAITING_FOR_APPROVAL"

  def notFoundRecipient: PartialFunction[ProjectableEvent, Future[UUID]] = { case x =>
    Future.failed(MessageRecipientNotFound(x.getClass.getName))
  }

  def notFoundPayload: PartialFunction[ProjectableEvent, Either[ComponentError, DynamoEventPayload]] = { case x =>
    Left(DynamoConverterNotFound(x.getClass.getName))
  }
}
