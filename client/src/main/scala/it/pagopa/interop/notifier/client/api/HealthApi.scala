/**
 * Notification Notifier Micro Service
 * This service notifies organization about occurred platform events
 *
 * The version of the OpenAPI document: {{version}}
 * Contact: support@example.com
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package it.pagopa.interop.notifier.client.api

import it.pagopa.interop.notifier.client.model.Problem
import it.pagopa.interop.notifier.client.invoker._
import it.pagopa.interop.notifier.client.invoker.CollectionFormats._
import it.pagopa.interop.notifier.client.invoker.ApiKeyLocations._

object HealthApi {

  def apply(baseUrl: String = "http://localhost/notifier/}") = new HealthApi(baseUrl)
}

class HealthApi(baseUrl: String) {

  /**
   * Return ok
   * 
   * Expected answers:
   *   code 200 : Problem (successful operation)
   * 
   * Available security schemes:
   *   bearerAuth (http)
   */
  def getStatus()(implicit bearerToken: BearerToken): ApiRequest[Problem] =
    ApiRequest[Problem](ApiMethods.GET, baseUrl, "/status", "application/json")
      .withCredentials(bearerToken)
      .withSuccessResponse[Problem](200)

}