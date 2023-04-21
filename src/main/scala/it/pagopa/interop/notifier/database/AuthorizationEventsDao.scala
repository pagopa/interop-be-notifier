package it.pagopa.interop.notifier.database

import it.pagopa.interop.authorizationmanagement.model.key.PersistentKey
import it.pagopa.interop.notifier.service.converters.EventType
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.PostgresProfile.api._
import slick.sql.{SqlAction, SqlStreamingAction}

import scala.concurrent.Future

object AuthorizationEventsDao {
  private final val db = Database.forConfig("keys-db")

  def select(lastEventId: Long, limit: Int): Future[Vector[KeyEventRecord]] = {
    val statement: SqlStreamingAction[Vector[KeyEventRecord], KeyEventRecord, Effect] =
      sql"SELECT * FROM keys WHERE eventId > $lastEventId ORDER BY eventId ASC LIMIT $limit ".as[KeyEventRecord]
    db.run(statement)
  }

  def insertKeys(keys: Seq[PersistentKey]): Future[Int] = {
    val values: String = keys.map(key => s"('${key.kid}','${EventType.ADDED.toString}')").mkString(",")
    val statement: SqlAction[Int, NoStream, Effect] = sqlu"INSERT INTO keys (kid,eventType) values $values"
    db.run(statement)
  }

  def deleteKey(kid: String): Future[Int] = {
    val statement: DBIOAction[Int, NoStream, Effect] = sqlu"DELETE FROM keys WHERE kid = '$kid'"
    db.run(statement)
  }
}
