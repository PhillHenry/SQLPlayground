import Dependencies._

ThisBuild / scalaVersion     := "3.4.0"
ThisBuild / version          := "0.1.0"
ThisBuild / organization     := "uk.co.odinconsultants"
ThisBuild / organizationName := "OdinConsultants"

ThisBuild / evictionErrorLevel := Level.Warn
ThisBuild / scalafixDependencies += Libraries.organizeImports

ThisBuild / resolvers += Resolver.sonatypeRepo("snapshots")

Compile / run / fork           := true

Global / onChangedBuildSource := ReloadOnSourceChanges
Global / semanticdbEnabled    := true // for metals

val commonSettings = List(
  scalacOptions ++= List("-source:future"),
  scalafmtOnCompile := false, // recommended in Scala 3
  testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
  libraryDependencies ++= Seq(
    Libraries.cats,
    Libraries.testkit,
    Libraries.quill,
//    Libraries.doobieQuill,
    Libraries.log4Cats,
    Libraries.sqlServer,
    Libraries.catsEffect,
    Libraries.kittens,
    Libraries.monocleCore.value,
    Libraries.neutronCore,
    Libraries.odin,
    Libraries.refinedCore.value,
    Libraries.refinedCats.value,
    Libraries.ip4s,
    Libraries.logBack,
    Libraries.documentationUtilsCore,
    Libraries.documentationUtilsScalaTest,
    Libraries.scalaTest,
    Libraries.monocleLaw          % Test,
    Libraries.scalacheck          % Test,
    Libraries.weaverCats          % Test,
    Libraries.weaverDiscipline    % Test,
    Libraries.weaverScalaCheck    % Test,
    Libraries.dockerJava          % Test,
    Libraries.dockerJavaTransport % Test,
  ),
)

def dockerSettings(name: String) = List(
  Docker / packageName := organizationName + "-" + name,
  dockerBaseImage      := "jdk17-curl:latest",
  dockerExposedPorts ++= List(8080),
  makeBatScripts       := Nil,
  dockerUpdateLatest   := true,
)

lazy val root = (project in file("."))
  .settings(
    name := "SQLPlayground"
  )
  .aggregate(lib, core, it)

lazy val lib = (project in file("modules/lib"))
  .settings(commonSettings: _*)

lazy val core = (project in file("modules/core"))
  .settings(commonSettings: _*)
  .dependsOn(lib)

// integration tests
lazy val it = (project in file("modules/it"))
  .settings(commonSettings: _*)
  .dependsOn(core)
  .settings(
    libraryDependencies ++= List(
      "ch.qos.logback" % "logback-classic" % "1.2.11" % Test
    )
  )

addCommandAlias("runLinter", ";scalafixAll --rules OrganizeImports")
