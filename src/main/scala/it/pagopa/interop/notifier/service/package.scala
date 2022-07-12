package it.pagopa.interop.notifier
import akka.actor.ActorSystem
import it.pagopa.interop._
import scala.concurrent.ExecutionContextExecutor

package object service {

  type PurposeManagementInvoker = purposemanagement.client.invoker.ApiInvoker
  type PurposeManagementApi     = purposemanagement.client.api.PurposeApi
  type CatalogManagementInvoker = catalogmanagement.client.invoker.ApiInvoker
  type CatalogManagementApi     = catalogmanagement.client.api.EServiceApi

  object PurposeManagementInvoker {
    def apply(blockingEc: ExecutionContextExecutor)(implicit actorSystem: ActorSystem): PurposeManagementInvoker =
      purposemanagement.client.invoker.ApiInvoker(purposemanagement.client.api.EnumsSerializers.all, blockingEc)
  }

  object PurposeManagementApi {
    def apply(baseUrl: String): PurposeManagementApi = purposemanagement.client.api.PurposeApi(baseUrl)
  }

  object CatalogManagementInvoker {
    def apply(blockingEc: ExecutionContextExecutor)(implicit actorSystem: ActorSystem): CatalogManagementInvoker =
      catalogmanagement.client.invoker.ApiInvoker(catalogmanagement.client.api.EnumsSerializers.all, blockingEc)
  }

  object CatalogManagementApi {
    def apply(baseUrl: String): CatalogManagementApi = catalogmanagement.client.api.EServiceApi(baseUrl)
  }
}
