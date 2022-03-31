package it.pagopa.interop.notifier.service.impl

import com.mongodb.client.model.Updates
import it.pagopa.interop.notifier.common.DatabaseConfiguration
import it.pagopa.interop.notifier.model.{Organization, OrganizationUpdatePayload}
import it.pagopa.interop.notifier.service.PersistenceService
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Updates.set
import org.mongodb.scala.{MongoClient, MongoCollection, MongoDatabase}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class MongoDBPersistentService(val dbConfig: DatabaseConfiguration)(implicit ec: ExecutionContext)
    extends PersistenceService {
  val mongoClient             = MongoClient(dbConfig.uri)
  val codecRegistry           = fromRegistries(fromProviders(classOf[MongoOrganization]), DEFAULT_CODEC_REGISTRY)
  val database: MongoDatabase = mongoClient.getDatabase(dbConfig.dbName).withCodecRegistry(codecRegistry)
  val collection: MongoCollection[MongoOrganization] = database.getCollection(dbConfig.dbCollection)

  override def addOrganization(organization: Organization): Future[Unit] =
    collection.insertOne(MongoOrganization.toMongo(organization)).toFuture().map(_ => ())

  override def updateOrganization(organizationId: UUID, updatePayload: OrganizationUpdatePayload): Future[Unit] = {
    val updateSequence: Bson =
      Updates.combine(set("notificationURL", updatePayload.notificationURL), set("audience", updatePayload.audience))

    collection
      .updateOne(equal("organizationId", organizationId.toString), updateSequence)
      .toFuture()
      .map(_ => ())
  }

  override def deleteOrganization(organizationId: UUID): Future[Unit] =
    collection.deleteOne(equal("organizationId", organizationId.toString)).toFuture().map(_ => ())

  override def getOrganization(organizationId: UUID): Future[Organization] =
    collection
      .find(equal("organizationId", organizationId.toString))
      .first()
      .toFuture()
      .map(x => {
        println(x.toString)
        MongoOrganization.fromMongo(x)
      })
}
