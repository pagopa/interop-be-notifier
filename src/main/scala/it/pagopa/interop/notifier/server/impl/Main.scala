package it.pagopa.interop.notifier.server.impl

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, DispatcherSelector}
import akka.cluster.ClusterEvent
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.typed.{Cluster, Subscribe}
import akka.http.scaladsl.Http
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import buildinfo.BuildInfo
import cats.syntax.all._
import com.typesafe.scalalogging.Logger
import it.pagopa.interop.commons.logging.renderBuildInfo
import it.pagopa.interop.commons.queue.QueueReader
import it.pagopa.interop.commons.utils.CORSSupport
import it.pagopa.interop.notifier.common.system.ApplicationConfiguration
import it.pagopa.interop.notifier.server.Controller
import it.pagopa.interop.notifier.service.impl._
import org.scanamo.ScanamoAsync
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

object Main extends App with CORSSupport with Dependencies {

  val logger: Logger = Logger(this.getClass)

  val system: ActorSystem[Nothing] = ActorSystem[Nothing](
    Behaviors.setup[Nothing] { context =>
      implicit val actorSystem: ActorSystem[Nothing]          = context.system
      implicit val executionContext: ExecutionContextExecutor = actorSystem.executionContext

      val selector: DispatcherSelector         = DispatcherSelector.fromConfig("futures-dispatcher")
      val blockingEc: ExecutionContextExecutor = actorSystem.dispatchers.lookup(selector)

      val (dynamoNotificationService, dynamoIndexService) = {
        val scanamo = ScanamoAsync(DynamoDbAsyncClient.create())(blockingEc)
        (new DynamoNotificationService(scanamo), new DynamoNotificationResourcesService(scanamo))
      }

      AkkaManagement.get(actorSystem.classicSystem).start()

      val sharding: ClusterSharding = ClusterSharding(actorSystem)
      sharding.init(organizationNotificationEntity)

      val cluster = Cluster(actorSystem)
      ClusterBootstrap.get(actorSystem.classicSystem).start()

      val listener = context.spawn(
        Behaviors.receive[ClusterEvent.MemberEvent]((ctx, event) => {
          ctx.log.info("MemberEvent: {}", event)
          Behaviors.same
        }),
        "listener"
      )

      cluster.subscriptions ! Subscribe(listener, classOf[ClusterEvent.MemberEvent])

      if (ApplicationConfiguration.projectionsEnabled) initProjections()

      logger.info(renderBuildInfo(BuildInfo))
      logger.info(s"Started cluster at ${cluster.selfMember.address}")

      val handler: QueueHandler = new QueueHandler(
        interopTokenGenerator = interopTokenGenerator(blockingEc),
        idRetriever = eventIdRetriever(sharding),
        dynamoNotificationService = dynamoNotificationService,
        dynamoIndexService = dynamoIndexService,
        catalogManagementService = catalogManagementService(blockingEc),
        authorizationEventsHandler = authorizationEventsHandler(blockingEc)
      )

      val queueReader: QueueReader = sqsReader()(blockingEc)

      val serverBinding: Future[Http.ServerBinding] = for {
        jwtReader <- getJwtReader()
        dynamo     = dynamoNotificationService
        events     = eventsApi(dynamo, jwtReader)
        controller = new Controller(events, healthApi, validationExceptionToRoute.some)(actorSystem.classicSystem)
        binding <- Http().newServerAt("0.0.0.0", ApplicationConfiguration.serverPort).bind(controller.routes)
      } yield binding

      serverBinding.onComplete {
        case Success(b) =>
          logger.info(s"Started server at ${b.localAddress.getHostString}:${b.localAddress.getPort}")
        case Failure(e) =>
          actorSystem.terminate()
          logger.error("Startup error: ", e)
      }

      queueReader.handle(handler.processMessage).onComplete {
        case Success(_) =>
          logger.error(s"SQSQueue handling somehow finished, and this should not have happened.")
        case Failure(e) =>
          actorSystem.terminate()
          logger.error("Startup error: ", e)
      }

      Behaviors.empty
    },
    BuildInfo.name
  )
}
