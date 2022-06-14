package it.pagopa.interop.notifier.server.impl

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorSystem
import akka.cluster.ClusterEvent
import akka.cluster.typed.{Cluster, Subscribe}
import akka.http.scaladsl.Http
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement

import it.pagopa.interop.commons.utils.CORSSupport
import it.pagopa.interop.notifier.common.system.ApplicationConfiguration
import it.pagopa.interop.notifier.server.Controller
import it.pagopa.interop.notifier.service.impl._
import kamon.Kamon

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext
import buildinfo.BuildInfo
import com.typesafe.scalalogging.Logger
import it.pagopa.interop.commons.logging.renderBuildInfo
import cats.syntax.all._
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.Future
import scala.util.{Success, Failure}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import it.pagopa.interop.commons.queue.QueueReader

object Main extends App with CORSSupport with Dependencies {

  val logger: Logger = Logger(this.getClass)

  val system: ActorSystem[Nothing] = ActorSystem[Nothing](
    Behaviors.setup[Nothing] { context =>
      implicit val actorSystem: ActorSystem[Nothing]          = context.system
      implicit val executionContext: ExecutionContextExecutor = actorSystem.executionContext

      Kamon.init()
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

      logger.info(renderBuildInfo(BuildInfo))
      logger.info(s"Started cluster at ${cluster.selfMember.address}")

      val handler: QueueHandler = new QueueHandler(
        interopTokenGenerator,
        eventIdRetriever(sharding),
        dynamoReader(),
        catalogManagementService(),
        purposeManagementService()
      )

      val readerExecutionContext: ExecutionContextExecutor =
        ExecutionContext.fromExecutor(Executors.newFixedThreadPool(ApplicationConfiguration.threadPoolSize))

      val queueReader: QueueReader = sqsReader()(readerExecutionContext)

      val serverBinding: Future[Http.ServerBinding] = for {
        jwtReader <- getJwtReader()
        dynamo     = dynamoReader()
        events     = eventsApi(dynamo, jwtReader)
        controller = new Controller(events, healthApi, validationExceptionToRoute.some)(actorSystem.classicSystem)
        binding <- Http().newServerAt("0.0.0.0", ApplicationConfiguration.serverPort).bind(controller.routes)
      } yield binding

      val queueHandling: Future[Unit] = queueReader.handle(handler.processMessage)

      serverBinding.onComplete {
        case Success(b) =>
          logger.info(s"Started server at ${b.localAddress.getHostString}:${b.localAddress.getPort}")
        case Failure(e) =>
          actorSystem.terminate()
          logger.error("Startup error: ", e)
      }

      queueHandling.onComplete {
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

  system.whenTerminated.onComplete(_ => Kamon.stop())(scala.concurrent.ExecutionContext.global)

}
