package it.pagopa.interop.notifier.common.system

import com.typesafe.config.{Config, ConfigFactory}

import scala.jdk.CollectionConverters.CollectionHasAsScala

object ApplicationConfiguration {
  lazy val config: Config               = ConfigFactory.load()
  lazy val serverPort: Int              = config.getInt("notifier.port")
  lazy val interopAudience: Set[String] = config.getStringList("notifier.jwt.audience").asScala.toSet
  def rsaPrivatePath: String            = config.getString("notifier.rsa-private-path")
}
