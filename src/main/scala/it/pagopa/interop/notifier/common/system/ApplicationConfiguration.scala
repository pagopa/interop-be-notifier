package it.pagopa.interop.notifier.common.system

import com.typesafe.config.{Config, ConfigFactory}
import it.pagopa.interop.commons.cqrs.model.MongoDbConfig

object ApplicationConfiguration {
  val config: Config                  = ConfigFactory.load()
  val serverPort: Int                 = config.getInt("notifier.port")
  val agreementManagementURL: String  = config.getString("notifier.services.agreement-management")
  val purposeManagementURL: String    = config.getString("notifier.services.purpose-management")
  val catalogManagementURL: String    = config.getString("notifier.services.catalog-management")
  val dynamoTableName: String         = config.getString("notifier.dynamo.table-name")
  val dynamoInvertedIndexName: String = config.getString("notifier.dynamo.inverted-index-name")
  val queueURL: String                = config.getString("notifier.queue.url")
  val threadPoolSize: Int             = config.getInt("notifier.queue.thread-pool-size")
  val numberOfProjectionTags: Int     = config.getInt("akka.cluster.sharding.number-of-shards")

  val signerMaxConnections: Int = config.getInt("notifier.signer-max-connections")

  val interopAudience: Set[String] =
    config.getString("notifier.jwt.audience").split(",").toSet.filter(_.nonEmpty)

  val rsaKeysIdentifiers: Set[String] =
    config.getString("notifier.rsa-keys-identifiers").split(",").toSet.filter(_.nonEmpty)

  val ecKeysIdentifiers: Set[String] =
    config.getString("notifier.ec-keys-identifiers").split(",").toSet.filter(_.nonEmpty)

  def projectionTag(index: Int) = s"interop-be-notifier-persistence|$index"

  val projectionsEnabled: Boolean = config.getBoolean("akka.projection.enabled")

  // Loaded only if projections are enabled
  lazy val mongoDb: MongoDbConfig = {
    val connectionString: String = config.getString("cqrs-projection.db.connection-string")
    val dbName: String           = config.getString("cqrs-projection.db.name")
    val collectionName: String   = config.getString("cqrs-projection.db.collection-name")

    MongoDbConfig(connectionString, dbName, collectionName)
  }

  require(interopAudience.nonEmpty, "Audience cannot be empty")
  require(
    rsaKeysIdentifiers.nonEmpty || ecKeysIdentifiers.nonEmpty,
    "You MUST provide at least one signing key (either RSA or EC)"
  )
}
