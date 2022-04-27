package it.pagopa.interop.notifier.common.system

import com.typesafe.config.{Config, ConfigFactory}

import scala.jdk.CollectionConverters.CollectionHasAsScala

object ApplicationConfiguration {
  lazy val config: Config            = ConfigFactory.load()
  val serverPort: Int                = config.getInt("notifier.port")
  val interopAudience: Set[String]   = config.getStringList("notifier.jwt.audience").asScala.toSet
  val rsaPrivatePath: String         = config.getString("notifier.rsa-private-path")
  val agreementManagementURL: String = config.getString("notifier.services.agreement-management")
  val purposeManagementURL: String   = config.getString("notifier.services.purpose-management")
  val catalogManagementURL: String   = config.getString("notifier.services.catalog-management")
  val dynamoTableName: String        = config.getString("notifier.dynamo.table-name")
  val queueURL: String               = config.getString("notifier.queue.url")
  val threadPoolSize: Int            = config.getInt("notifier.queue.thread-pool-size")
}
