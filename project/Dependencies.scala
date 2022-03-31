import PagopaVersions._
import Versions._
import sbt._

object Dependencies {

  private[this] object akka {
    lazy val namespace     = "com.typesafe.akka"
    lazy val actorTyped    = namespace                       %% "akka-actor-typed"           % akkaVersion
    lazy val actor         = namespace                       %% "akka-actor"                 % akkaVersion
    lazy val serialization = namespace                       %% "akka-serialization-jackson" % akkaVersion
    lazy val stream        = namespace                       %% "akka-stream"                % akkaVersion
    lazy val clusterTools  = namespace                       %% "akka-cluster-tools"         % akkaVersion
    lazy val http          = namespace                       %% "akka-http"                  % akkaHttpVersion
    lazy val httpJson      = namespace                       %% "akka-http-spray-json"       % akkaHttpVersion
    lazy val httpJson4s    = "de.heikoseeberger"             %% "akka-http-json4s"           % akkaHttpJson4sVersion
    lazy val management    = "com.lightbend.akka.management" %% "akka-management"            % akkaManagementVersion
    lazy val managementLogLevels =
      "com.lightbend.akka.management" %% "akka-management-loglevels-logback" % akkaManagementVersion
    lazy val slf4j         = namespace %% "akka-slf4j"          % akkaVersion
    lazy val httpTestkit   = namespace %% "akka-http-testkit"   % akkaHttpVersion
    lazy val streamTestkit = namespace %% "akka-stream-testkit" % akkaVersion
    lazy val testkit       = namespace %% "akka-testkit"        % akkaVersion
  }

  private[this] object mongo {
    lazy val driver = "org.mongodb.scala" %% "mongo-scala-driver" % mongoDBVersion
  }

  private[this] object aws {
    lazy val dynamodb = "software.amazon.awssdk" % "dynamodb" % awsDynamoDBVersion
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

  private[this] object json4s {
    lazy val namespace = "org.json4s"
    lazy val jackson   = namespace %% "json4s-jackson" % json4sVersion
    lazy val ext       = namespace %% "json4s-ext"     % json4sVersion
  }

  private[this] object logback {
    lazy val namespace = "ch.qos.logback"
    lazy val classic   = namespace % "logback-classic" % logbackVersion
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
      akka.actor                  % Compile,
      akka.actorTyped             % Compile,
      akka.clusterTools           % Compile,
      akka.http                   % Compile,
      akka.httpJson               % Compile,
      akka.management             % Compile,
      akka.managementLogLevels    % Compile,
      akka.serialization          % Compile,
      akka.slf4j                  % Compile,
      akka.stream                 % Compile,
      aws.dynamodb                % Compile,
      cats.core                   % Compile,
      kamon.core                  % Compile,
      kamon.statusPage            % Compile,
      kamon.systemMetrics         % Compile,
      kamon.executors             % Compile,
      kamon.akka                  % Compile,
      kamon.akkaHttp              % Compile,
      kamon.instrumentationCommon % Compile,
      kamon.scalaFuture           % Compile,
      kamon.logback               % Compile,
      kamon.jdbc                  % Compile,
      kamon.prometheus            % Compile,
      logback.classic             % Compile,
      mongo.driver                % Compile,
      mustache.mustache           % Compile,
      pagopa.commons              % Compile,
      pagopa.jwt                  % Compile,
      pagopa.vault                % Compile,
      akka.httpTestkit            % Test,
      akka.streamTestkit          % Test,
      akka.testkit                % Test,
      scalatest.core              % Test,
      scalamock.core              % Test
    )
    lazy val client: Seq[ModuleID]    =
      Seq(
        akka.stream     % Compile,
        akka.http       % Compile,
        akka.httpJson4s % Compile,
        akka.slf4j      % Compile,
        json4s.jackson  % Compile,
        json4s.ext      % Compile
      )
  }
}
