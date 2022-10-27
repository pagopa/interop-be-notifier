package it.pagopa.interop.notifier

import akka.actor
import akka.actor.testkit.typed.scaladsl.{ActorTestKit, ScalaTestWithActorTestKit}
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityRef}
import akka.cluster.typed.{Cluster, Join}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.server.directives.{AuthenticationDirective, SecurityDirectives}
import it.pagopa.interop.commons.utils.AkkaUtils.getShard
import it.pagopa.interop.notifier.api._
import it.pagopa.interop.notifier.api.impl._
import it.pagopa.interop.notifier.common.system.ApplicationConfiguration
import it.pagopa.interop.notifier.model.persistence.{
  Command,
  OrganizationNotificationEventIdBehavior,
  PersistentOrganizationEvent,
  UpdateOrganizationNotificationEventId
}
import it.pagopa.interop.notifier.server.Controller
import it.pagopa.interop.notifier.server.impl.Dependencies
import it.pagopa.interop.notifier.service.impl.DynamoNotificationService
import org.scalamock.scalatest.MockFactory
import org.scanamo.ScanamoAsync
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import spray.json._

import java.net.InetAddress
import java.util.UUID
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor, Future}

trait ItSpecHelper
    extends ItSpecConfiguration
    with ItCqrsSpec
    with MockFactory
    with SprayJsonSupport
    with DefaultJsonProtocol
    with Dependencies {
  self: ScalaTestWithActorTestKit =>

  val bearerToken: String                   = "token"
  final val requestHeaders: Seq[HttpHeader] =
    Seq(
      headers.Authorization(OAuth2BearerToken("token")),
      headers.RawHeader("X-Correlation-Id", "test-id"),
      headers.`X-Forwarded-For`(RemoteAddress(InetAddress.getByName("127.0.0.1")))
    )

  val mockDynamoNotificationService: DynamoNotificationService = mock[DynamoNotificationService]

  val healthApiMock: HealthApi = mock[HealthApi]

  val apiMarshaller: EventsApiMarshaller = EventsApiMarshallerImpl

  var controller: Option[Controller]                 = None
  var bindServer: Option[Future[Http.ServerBinding]] = None

  val wrappingDirective: AuthenticationDirective[Seq[(String, String)]] =
    SecurityDirectives.authenticateOAuth2("SecurityRealm", AdminMockAuthenticator)

  val sharding: ClusterSharding = ClusterSharding(system)

  val httpSystem: ActorSystem[Any]                        =
    ActorSystem(Behaviors.ignore[Any], name = system.name, config = system.settings.config)
  implicit val executionContext: ExecutionContextExecutor = httpSystem.executionContext
  val classicSystem: actor.ActorSystem                    = httpSystem.classicSystem
  implicit val scanamo: ScanamoAsync                      = ScanamoAsync(DynamoDbAsyncClient.create())

  override def startServer(): Unit = {
    val persistentEntity: Entity[Command, ShardingEnvelope[Command]] =
      Entity(OrganizationNotificationEventIdBehavior.TypeKey)(notificationBehaviorFactory)

    Cluster(system).manager ! Join(Cluster(system).selfMember.address)
    sharding.init(persistentEntity)

    val attributeApi =
      new EventsApi(new EventsServiceApiImpl(mockDynamoNotificationService), apiMarshaller, wrappingDirective)

    if (ApplicationConfiguration.projectionsEnabled) initProjections()

    controller = Some(new Controller(attributeApi, healthApiMock)(classicSystem))

    controller foreach { controller =>
      bindServer = Some(
        Http()(classicSystem)
          .newServerAt("0.0.0.0", 18088)
          .bind(controller.routes)
      )

      Await.result(bindServer.get, 100.seconds)
    }
  }

  override def shutdownServer(): Unit = {
    bindServer.foreach(_.foreach(_.unbind()))
    ActorTestKit.shutdown(httpSystem, 5.seconds)
  }

  def updateOrganizationEventId(organizationId: UUID): Option[PersistentOrganizationEvent] = {
    val commander: EntityRef[Command] =
      sharding.entityRefFor(
        OrganizationNotificationEventIdBehavior.TypeKey,
        getShard(organizationId.toString, ApplicationConfiguration.numberOfProjectionTags)
      )

    commander.ask(ref => UpdateOrganizationNotificationEventId(organizationId.toString(), ref)).futureValue
  }
}
