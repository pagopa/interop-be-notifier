package it.pagopa.interop.notifier.model.persistence.projection

import akka.actor.typed.ActorSystem
import it.pagopa.interop.commons.cqrs.model._
import it.pagopa.interop.commons.cqrs.service.CqrsProjection
import it.pagopa.interop.notifier.model.persistence._
import org.mongodb.scala.model._
import org.mongodb.scala.{MongoCollection, _}
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext

object NotifierCqrsProjection {
  def projection(offsetDbConfig: DatabaseConfig[JdbcProfile], mongoDbConfig: MongoDbConfig, projectionId: String)(
    implicit
    system: ActorSystem[_],
    ec: ExecutionContext
  ): CqrsProjection[Event] =
    CqrsProjection[Event](offsetDbConfig, mongoDbConfig, projectionId, eventHandler)

  private def eventHandler(collection: MongoCollection[Document], event: Event): PartialMongoAction = event match {
    case EventIdAdded(orgId, eventId) =>
      ActionWithDocument(
        collection.replaceOne(Filters.eq("data.id", orgId), _, ReplaceOptions().upsert(true)),
        Document(s"{ data: { id: \"$orgId\", eventId: $eventId } }")
      )
  }

}
