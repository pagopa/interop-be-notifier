package it.pagopa.interop.notifier
import akka.actor.ActorSystem
import it.pagopa.interop._

import scala.concurrent.ExecutionContextExecutor

package object service {

  type CatalogProcessInvoker = catalogprocess.client.invoker.ApiInvoker
  type CatalogProcessApi     = catalogprocess.client.api.ProcessApi

  object CatalogProcessInvoker {
    def apply(blockingEc: ExecutionContextExecutor)(implicit actorSystem: ActorSystem): CatalogProcessInvoker =
      catalogprocess.client.invoker.ApiInvoker(catalogprocess.client.api.EnumsSerializers.all, blockingEc)
  }

  object CatalogProcessApi {
    def apply(baseUrl: String): CatalogProcessApi = catalogprocess.client.api.ProcessApi(baseUrl)
  }
}
