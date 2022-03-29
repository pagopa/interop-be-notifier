package it.pagopa.interop.notifier.common

import com.typesafe.config.{Config, ConfigFactory}

import scala.jdk.CollectionConverters.ListHasAsScala

object ApplicationConfiguration {
  lazy val config: Config = ConfigFactory.load()

  lazy val serverPort: Int = config.getInt("notifier.port")

  lazy val getInteropIdIssuer: String = config.getString("notifier.issuer")

  lazy val jwtAudience: Set[String] = config.getStringList("notifier.jwt.audience").asScala.toSet
}
