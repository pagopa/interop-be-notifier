package it.pagopa.interop.notifier.common

import com.typesafe.config.{Config, ConfigFactory}

import scala.jdk.CollectionConverters.ListHasAsScala

case class DatabaseConfiguration(uri: String, dbName: String, dbCollection: String)
object ApplicationConfiguration {
  lazy val config: Config = ConfigFactory.load()

  lazy val serverPort: Int            = config.getInt("notifier.port")
  lazy val getInteropIdIssuer: String = config.getString("notifier.issuer")
  lazy val jwtAudience: Set[String]   = config.getStringList("notifier.jwt.audience").asScala.toSet

  lazy val dbConfiguration = DatabaseConfiguration(
    config.getString("notifier.document-db.uri"),
    config.getString("notifier.document-db.database.name"),
    config.getString("notifier.document-db.database.collection")
  )

}
