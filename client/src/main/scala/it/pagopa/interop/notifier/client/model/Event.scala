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
package it.pagopa.interop.notifier.client.model

import it.pagopa.interop.notifier.client.invoker.ApiModel

case class Event(eventId: Long, eventType: String, objectType: String, objectId: Map[String, String]) extends ApiModel