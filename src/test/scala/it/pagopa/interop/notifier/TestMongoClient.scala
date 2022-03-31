package it.pagopa.interop.notifier

import com.typesafe.config.{Config, ConfigFactory}
import it.pagopa.interop.notifier.common.DatabaseConfiguration
import it.pagopa.interop.notifier.model.{Organization, OrganizationUpdatePayload}
import it.pagopa.interop.notifier.service.impl.MongoDBPersistentService

import java.util.UUID
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
import scala.language.postfixOps

object TestMongoClient extends App {
  import ExecutionContext.Implicits.global
  println("*******")

  val config: Config = ConfigFactory
    .parseResourcesAnySyntax("application")
    .resolve()

  val dbConfiguration = DatabaseConfiguration(
    config.getString("notifier.document-db.uri"),
    config.getString("notifier.document-db.database.name"),
    config.getString("notifier.document-db.database.collection")
  )

  val persistence = new MongoDBPersistentService(dbConfiguration)

  val organizationId = UUID.randomUUID()
  val execution      = for {
    _ <- persistence.addOrganization(Organization("test", "test", organizationId))
    _ = println("added")
    org <- persistence.getOrganization(organizationId)
    _ = println(org.notificationURL)
    _ <- persistence.updateOrganization(
      organizationId,
      OrganizationUpdatePayload(notificationURL = "pippolo", audience = "puppolo")
    )
    _ = println("updated! *************")
    updated <- persistence.getOrganization(organizationId)
    _ = println(updated.notificationURL + " " + updated.audience)
    _ <- persistence.deleteOrganization(organizationId)
  } yield ()

  Await.result(execution, 100 seconds)

}
