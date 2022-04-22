package it.pagopa.interop.notifier
import akka.actor.ActorSystem
import it.pagopa.interop._

package object service {

  type AgreementManagementInvoker = agreementmanagement.client.invoker.ApiInvoker
  type AgreementManagementApi     = agreementmanagement.client.api.AgreementApi
  type PurposeManagementInvoker   = purposemanagement.client.invoker.ApiInvoker
  type PurposeManagementApi       = purposemanagement.client.api.PurposeApi
  type CatalogManagementInvoker   = catalogmanagement.client.invoker.ApiInvoker
  type CatalogManagementApi       = catalogmanagement.client.api.EServiceApi

  object AgreementManagementInvoker {
    def apply()(implicit actorSystem: ActorSystem): AgreementManagementInvoker =
      agreementmanagement.client.invoker.ApiInvoker(agreementmanagement.client.api.EnumsSerializers.all)
  }

  object AgreementManagementApi {
    def apply(baseUrl: String): AgreementManagementApi = agreementmanagement.client.api.AgreementApi(baseUrl)
  }

  object PurposeManagementInvoker {
    def apply()(implicit actorSystem: ActorSystem): PurposeManagementInvoker =
      purposemanagement.client.invoker.ApiInvoker(purposemanagement.client.api.EnumsSerializers.all)
  }

  object PurposeManagementApi {
    def apply(baseUrl: String): PurposeManagementApi = purposemanagement.client.api.PurposeApi(baseUrl)
  }

  object CatalogManagementInvoker {
    def apply()(implicit actorSystem: ActorSystem): CatalogManagementInvoker =
      catalogmanagement.client.invoker.ApiInvoker(catalogmanagement.client.api.EnumsSerializers.all)
  }

  object CatalogManagementApi {
    def apply(baseUrl: String): CatalogManagementApi = catalogmanagement.client.api.EServiceApi(baseUrl)
  }

}
