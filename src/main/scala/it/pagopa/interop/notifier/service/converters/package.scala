package it.pagopa.interop.notifier.service

import it.pagopa.interop.commons.queue.message._
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.commons.utils.errors.ComponentError
import it.pagopa.interop.notifier.error.NotifierErrors.{
  DynamoConverterNotFound,
  MessageRecipientNotFound,
  OrganizationIdNotFound
}
import it.pagopa.interop.notifier.model.{MessageId, NotificationPayload}
import it.pagopa.interop.notifier.service.impl.DynamoNotificationResourcesService

import scala.concurrent.{ExecutionContext, Future}

package object converters {

  final val allOrganizations: String = "all_organizations"

  def notFoundRecipient: Message => Future[Unit] = message =>
    Future.failed(MessageRecipientNotFound(message.payload.getClass.getName))

  def notFoundPayload: ProjectableEvent => Either[ComponentError, Option[NotificationPayload]] =
    x => Left(DynamoConverterNotFound(x.getClass.getName))

  object EventType extends Enumeration {
    type EventType = Value
    val ADDED, CREATED, CLONED, DELETED, UPDATED, SUSPENDED, ACTIVATED, ARCHIVED, WAITING_FOR_APPROVAL = Value
  }

  def getMessageIdFromDynamo(
    dynamoIndexService: DynamoNotificationResourcesService
  )(resourceId: String)(implicit ec: ExecutionContext, contexts: Seq[(String, String)]): Future[MessageId] = for {
    found     <- dynamoIndexService.getOne(resourceId)
    messageId <- found.toFuture(OrganizationIdNotFound(resourceId))
  } yield messageId
}
