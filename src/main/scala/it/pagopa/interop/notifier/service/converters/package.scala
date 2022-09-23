package it.pagopa.interop.notifier.service

import it.pagopa.interop.commons.queue.message.ProjectableEvent
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.commons.utils.errors.ComponentError
import it.pagopa.interop.notifier.error.NotifierErrors.{
  DynamoConverterNotFound,
  MessageRecipientNotFound,
  OrganizationIdNotFound
}
import it.pagopa.interop.notifier.model.{IndexQuery, MessageId, NotificationPayload}
import it.pagopa.interop.notifier.service.impl.DynamoIndexService

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

package object converters {

  def notFoundRecipient: PartialFunction[ProjectableEvent, Future[MessageId]] = { case x =>
    Future.failed(MessageRecipientNotFound(x.getClass.getName))
  }

  def notFoundPayload: PartialFunction[ProjectableEvent, Either[ComponentError, NotificationPayload]] = { case x =>
    Left(DynamoConverterNotFound(x.getClass.getName))
  }

  object EventType extends Enumeration {
    type EventType = Value
    val ADDED, CREATED, DELETED, UPDATED, SUSPENDED, DEACTIVATED, ACTIVATED, ARCHIVED, WAITING_FOR_APPROVAL = Value
  }

  def getMessageIdFromDynamo(
    dynamoIndexService: DynamoIndexService
  )(resourceId: UUID)(implicit ec: ExecutionContext, contexts: Seq[(String, String)]): Future[MessageId] = for {
    found     <- dynamoIndexService.getOne(IndexQuery(resourceId))
    messageId <- found.toFuture(OrganizationIdNotFound(resourceId.toString))
  } yield messageId
}
