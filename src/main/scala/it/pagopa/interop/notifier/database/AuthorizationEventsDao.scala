package it.pagopa.interop.notifier.database

import com.typesafe.scalalogging.Logger
import it.pagopa.interop.notifier.common.system.ApplicationConfiguration
import it.pagopa.interop.notifier.common.system.ApplicationConfiguration.postgresKeysNotificationTable
import it.pagopa.interop.notifier.service.converters.EventType.EventType
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.PostgresProfile.api._
import slick.sql.SqlStreamingAction

import scala.concurrent.Future

object AuthorizationEventsDao {

  private val logger: Logger = Logger(this.getClass)

  private final val postgresqlDB: Database =
    Database.forConfig(path = "notifier.postgres", config = ApplicationConfiguration.config)

  def select(lastEventId: Long, limit: Int): Future[Vector[KeyEventRecord]] = {
    logger.debug(s"Getting keys events from lastEventId ${lastEventId.toString} with limit ${limit.toString}")

    val statement: SqlStreamingAction[Vector[KeyEventRecord], KeyEventRecord, Effect] =
      sql"SELECT event_id, kid, event_type FROM #$postgresKeysNotificationTable WHERE event_id > $lastEventId ORDER BY event_id ASC LIMIT $limit"
        .as[KeyEventRecord]
    postgresqlDB.run(statement)
  }

  def insertKeysNotifications(kids: List[String], eventType: EventType): Future[List[Int]] = {
    logger.debug(s"Adding ${kids.mkString(",")} keys with eventType ${eventType.toString}")
    val inserts: List[DBIO[Int]]                                = kids.map(kid => createInsertStatement(kid, eventType))
    val statements: DBIOAction[List[Int], NoStream, Effect.All] = DBIO.sequence(inserts)
    postgresqlDB.run(statements)
  }

  def insertKeyNotification(kid: String, eventType: EventType): Future[Int] = {
    logger.debug(s"Adding $kid keys with eventType ${eventType.toString}")
    postgresqlDB.run(createInsertStatement(kid, eventType))
  }

  private def createInsertStatement(kid: String, eventType: EventType): DBIO[Int] =
    sqlu"INSERT INTO #$postgresKeysNotificationTable (kid, event_type) values ($kid, ${eventType.toString})"

}
