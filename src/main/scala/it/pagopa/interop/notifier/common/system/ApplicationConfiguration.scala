package it.pagopa.interop.notifier.common.system

import com.typesafe.config.{Config, ConfigFactory}

import scala.jdk.CollectionConverters.CollectionHasAsScala

object ApplicationConfiguration {
  lazy val config: Config               = ConfigFactory.load()
  lazy val serverPort: Int              = config.getInt("notifier.port")
  lazy val interopAudience: Set[String] = config.getStringList("notifier.jwt.audience").asScala.toSet
  def rsaPrivatePath: String            = config.getString("notifier.rsa-private-path")

  lazy val agreementManagementURL: String = config.getString("notifier.services.agreement-management")
  lazy val purposeManagementURL: String   = config.getString("notifier.services.purpose-management")
  lazy val catalogManagementURL: String   = config.getString("notifier.services.catalog-management")

  lazy val dynamoTableName: String = config.getString("notifier.dynamo.table-name")
  lazy val queueURL: String        = config.getString("notifier.queue.url")
}
