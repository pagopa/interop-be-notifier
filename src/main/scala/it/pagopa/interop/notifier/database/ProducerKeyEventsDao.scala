package it.pagopa.interop.notifier.database

import com.typesafe.scalalogging.Logger
import it.pagopa.interop.notifier.common.system.ApplicationConfiguration
import it.pagopa.interop.notifier.common.system.ApplicationConfiguration.postgresProducerKeysNotificationTable
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.PostgresProfile.api._
import slick.sql.SqlStreamingAction

import scala.concurrent.Future

object ProducerKeyEventsDao {

  private val logger: Logger = Logger(this.getClass)

  private final val postgresqlDB: Database =
    Database.forConfig(path = "notifier.postgres", config = ApplicationConfiguration.config)

  def select(lastEventId: Long, limit: Int): Future[Vector[ProducerKeyEventRecord]] = {
    logger.debug(s"Getting keys events from lastEventId ${lastEventId.toString} with limit ${limit.toString}")

    val statement: SqlStreamingAction[Vector[ProducerKeyEventRecord], ProducerKeyEventRecord, Effect] =
      sql"SELECT event_id, kid, event_type FROM #$postgresProducerKeysNotificationTable WHERE event_id > $lastEventId ORDER BY event_id ASC LIMIT $limit"
        .as[ProducerKeyEventRecord]
    postgresqlDB.run(statement)
  }

}
