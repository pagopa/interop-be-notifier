package it.pagopa.interop.notifier.error

import akka.http.scaladsl.server.StandardRoute
import com.typesafe.scalalogging.LoggerTakingImplicit
import it.pagopa.interop.commons.logging.ContextFieldsToLog
import it.pagopa.interop.commons.utils.errors.{AkkaResponses, GenericComponentErrors, ServiceCode}

import scala.util.{Failure, Try}

object Handlers extends AkkaResponses {

  implicit val serviceCode: ServiceCode = ServiceCode("017")

  def handleGetEventsFromIdError(logMessage: String)(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog]
  ): PartialFunction[Try[_], StandardRoute] = { case Failure(ex) =>
    internalServerError(ex, logMessage)
  }

  def handleGetAllEventsFromIdError(logMessage: String)(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog]
  ): PartialFunction[Try[_], StandardRoute] = { case Failure(ex) =>
    internalServerError(ex, logMessage)
  }
}
