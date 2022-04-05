package it.pagopa.interop.notifier.common.system

import com.typesafe.config.{Config, ConfigFactory}

import scala.jdk.CollectionConverters.CollectionHasAsScala

case class DatabaseConfiguration(uri: String, dbName: String, dbCollection: String)
object ApplicationConfiguration {
  lazy val config: Config               = ConfigFactory.load()
  lazy val serverPort: Int              = config.getInt("notifier.port")
  lazy val interopAudience: Set[String] = config.getStringList("notifier.jwt.audience").asScala.toSet
  def rsaPrivatePath: String            = config.getString("notifier.rsa-private-path")

  lazy val dbConfiguration = DatabaseConfiguration(
    config.getString("notifier.document-db.uri"),
    config.getString("notifier.document-db.database.name"),
    config.getString("notifier.document-db.database.collection")
  )

}
