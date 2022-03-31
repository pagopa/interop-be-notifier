package it.pagopa.interop.notifier.service

import akka.http.scaladsl.model.HttpResponse
import it.pagopa.interop.notifier.model.{Notification, Organization}

import scala.concurrent.Future

trait EventNotifier {
  def notifyAndForget(notification: Notification, organization: Organization): Future[HttpResponse]
}
