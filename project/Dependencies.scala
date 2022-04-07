import PagopaVersions._
import Versions._
import sbt._

object Dependencies {

  private[this] object akka {
    lazy val namespace              = "com.typesafe.akka"
    lazy val actorTyped             = namespace            %% "akka-actor-typed"             % akkaVersion
    lazy val clusterTyped           = namespace            %% "akka-cluster-typed"           % akkaVersion
    lazy val clusterSharding        = namespace            %% "akka-cluster-sharding-typed"  % akkaVersion
    lazy val discovery              = namespace            %% "akka-discovery"               % akkaVersion
    lazy val persistence            = namespace            %% "akka-persistence-typed"       % akkaVersion
    lazy val persistenceQuery       = namespace            %% "akka-persistence-query"       % akkaVersion
    lazy val projection             = "com.lightbend.akka" %% "akka-projection-eventsourced" % projectionVersion
    lazy val projectionSlick        = "com.lightbend.akka" %% "akka-projection-slick"        % slickProjectionVersion
    lazy val clusterTools           = namespace            %% "akka-cluster-tools"           % akkaVersion
    lazy val persistenceJdbc        = "com.lightbend.akka" %% "akka-persistence-jdbc"        % jdbcPersistenceVersion
    lazy val slick                  = "com.typesafe.slick" %% "slick"                        % slickVersion
    lazy val slickHikari            = "com.typesafe.slick" %% "slick-hikaricp"               % slickVersion
    lazy val s3Journal              = "com.github.j5ik2o"  %% "akka-persistence-s3-journal"  % s3Persistence
    lazy val s3Snapshot             = "com.github.j5ik2o"  %% "akka-persistence-s3-snapshot" % s3Persistence
    lazy val stream                 = namespace            %% "akka-stream-typed"            % akkaVersion
    lazy val http                   = namespace            %% "akka-http"                    % akkaHttpVersion
    lazy val httpJson               = namespace            %% "akka-http-spray-json"         % akkaHttpVersion
    lazy val httpJson4s             = "de.heikoseeberger"  %% "akka-http-json4s"             % httpJson4sVersion
    lazy val discoveryKubernetesApi =
      "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % akkaManagementVersion
    lazy val clusterBootstrap =
      "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % akkaManagementVersion
    lazy val clusterHttp = "com.lightbend.akka.management" %% "akka-management-cluster-http" % akkaManagementVersion
    lazy val slf4j       = namespace                       %% "akka-slf4j"                   % akkaVersion
    lazy val testkit     = namespace                       %% "akka-actor-testkit-typed"     % akkaVersion
    lazy val management  = "com.lightbend.akka.management" %% "akka-management"              % akkaManagementVersion
    lazy val managementLogLevels =
      "com.lightbend.akka.management" %% "akka-management-loglevels-logback" % akkaManagementVersion
  }

  private[this] object mongo {
    lazy val driver = "org.mongodb.scala" %% "mongo-scala-driver" % mongoDBVersion
  }

  private[this] object aws {
    lazy val dynamodb = "software.amazon.awssdk" % "dynamodb" % awsDynamoDBVersion
    lazy val sqs      = "software.amazon.awssdk" % "sqs"      % awsSqsVersion
  }

  private[this] object spray {
    lazy val spray = "io.spray" %% "spray-json" % sprayJsonVersion
  }

  private[this] object pagopa {
    lazy val namespace = "it.pagopa"
    lazy val commons   = namespace %% "interop-commons-utils" % commonsVersion
    lazy val jwt       = namespace %% "interop-commons-jwt"   % commonsVersion
    lazy val vault     = namespace %% "interop-commons-vault" % commonsVersion
  }

  private[this] object cats {
    lazy val namespace = "org.typelevel"
    lazy val core      = namespace %% "cats-core" % catsVersion
  }

  private[this] object logback {
    lazy val namespace = "ch.qos.logback"
    lazy val classic   = namespace % "logback-classic" % logbackVersion
  }

  lazy val Protobuf = "protobuf"

  private[this] object scalaprotobuf {
    lazy val namespace = "com.thesamet.scalapb"
    lazy val core      = namespace %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion
  }

  private[this] object kamon {
    lazy val namespace             = "io.kamon"
    lazy val core                  = namespace %% "kamon-core"                   % kamonVersion
    lazy val prometheus            = namespace %% "kamon-prometheus"             % kamonVersion
    lazy val statusPage            = namespace %% "kamon-status-page"            % kamonVersion
    lazy val systemMetrics         = namespace %% "kamon-system-metrics"         % kamonVersion
    lazy val executors             = namespace %% "kamon-executors"              % kamonVersion
    lazy val akka                  = namespace %% "kamon-akka"                   % kamonVersion
    lazy val akkaHttp              = namespace %% "kamon-akka-http"              % kamonVersion
    lazy val instrumentationCommon = namespace %% "kamon-instrumentation-common" % kamonVersion
    lazy val scalaFuture           = namespace %% "kamon-scala-future"           % kamonVersion
    lazy val logback               = namespace %% "kamon-logback"                % kamonVersion
    lazy val jdbc                  = namespace %% "kamon-jdbc"                   % kamonVersion
  }

  private[this] object mustache {
    lazy val mustache = "com.github.spullara.mustache.java" % "compiler" % mustacheVersion
  }

  private[this] object scalatest {
    lazy val namespace = "org.scalatest"
    lazy val core      = namespace %% "scalatest" % scalatestVersion
  }

  private[this] object scalamock {
    lazy val namespace = "org.scalamock"
    lazy val core      = namespace %% "scalamock" % scalaMockVersion
  }

  private[this] object jackson {
    lazy val namespace   = "com.fasterxml.jackson.core"
    lazy val core        = namespace % "jackson-core"         % jacksonVersion
    lazy val annotations = namespace % "jackson-annotations"  % jacksonVersion
    lazy val databind    = namespace % "jackson-databind"     % jacksonVersion
    lazy val scalaModule = namespace % "jackson-module-scala" % jacksonVersion
  }

  object Jars {
    lazy val overrides: Seq[ModuleID] =
      Seq(
        jackson.annotations % Compile,
        jackson.core        % Compile,
        jackson.databind    % Compile,
        jackson.scalaModule % Compile
      )
    lazy val `server`: Seq[ModuleID]  = Seq(
      // For making Java 12 happy
      "javax.annotation"          % "javax.annotation-api" % "1.3.2" % "compile",
      //
      akka.actorTyped             % Compile,
      akka.clusterBootstrap       % Compile,
      akka.clusterHttp            % Compile,
      akka.clusterSharding        % Compile,
      akka.clusterTools           % Compile,
      akka.clusterTyped           % Compile,
      akka.discovery              % Compile,
      akka.discoveryKubernetesApi % Compile,
      akka.http                   % Compile,
      akka.httpJson               % Compile,
      akka.management             % Compile,
      akka.managementLogLevels    % Compile,
      akka.persistence            % Compile,
      akka.persistenceJdbc        % Compile,
      akka.persistenceQuery       % Compile,
      akka.projection             % Compile,
      akka.projectionSlick        % Compile,
      akka.s3Journal              % Compile,
      akka.s3Snapshot             % Compile,
      akka.slf4j                  % Compile,
      akka.slick                  % Compile,
      akka.slickHikari            % Compile,
      akka.stream                 % Compile,
      aws.dynamodb                % Compile,
      aws.sqs                     % Compile,
      cats.core                   % Compile,
      kamon.akka                  % Compile,
      kamon.akkaHttp              % Compile,
      kamon.core                  % Compile,
      kamon.executors             % Compile,
      kamon.instrumentationCommon % Compile,
      kamon.jdbc                  % Compile,
      kamon.logback               % Compile,
      kamon.prometheus            % Compile,
      kamon.scalaFuture           % Compile,
      kamon.statusPage            % Compile,
      kamon.systemMetrics         % Compile,
      logback.classic             % Compile,
      mongo.driver                % Compile,
      mustache.mustache           % Compile,
      pagopa.commons              % Compile,
      pagopa.jwt                  % Compile,
      pagopa.vault                % Compile,
      scalaprotobuf.core          % Protobuf,
      akka.testkit                % Test,
      scalatest.core              % Test,
      scalamock.core              % Test
    )
    lazy val commons: Seq[ModuleID]   = List(spray.spray % Compile)
    lazy val client: Seq[ModuleID]    =
      Seq(akka.http % Compile, akka.httpJson4s % Compile, aws.sqs % Compile)
  }
}
