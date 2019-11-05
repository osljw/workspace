ThisBuild / resolvers ++= Seq(
    "Apache Development Snapshot Repository" at "https://repository.apache.org/content/repositories/snapshots/",
    Resolver.mavenLocal
)

name := "flink_test"

version := "0.1-SNAPSHOT"

organization := "org.test"

ThisBuild / scalaVersion := "2.11.12"

val flinkVersion = "1.8.1"

val flinkDependencies = Seq(
  "org.apache.flink" %% "flink-scala" % flinkVersion % "provided",
  "org.apache.flink" %% "flink-streaming-scala" % flinkVersion % "provided")

lazy val root = (project in file(".")).
  settings(
    libraryDependencies ++= flinkDependencies
  )

//libraryDependencies += "org.apache.flink" %% "flink-connector-kafka-0.8" % "1.7.2"
libraryDependencies += "org.apache.flink" %% "flink-connector-kafka" % "1.7.2"
libraryDependencies += "org.apache.logging.log4j" % "log4j-core" % "2.12.0"
libraryDependencies += "log4j" % "log4j" % "1.2.17"

assembly / mainClass := Some("org.test.Job")

// make run command include the provided dependencies
Compile / run  := Defaults.runTask(Compile / fullClasspath,
                                   Compile / run / mainClass,
                                   Compile / run / runner
                                  ).evaluated

// stays inside the sbt console when we press "ctrl-c" while a Flink programme executes with "run" or "runMain"
Compile / run / fork := true
Global / cancelable := true

// exclude Scala library from assembly
assembly / assemblyOption  := (assembly / assemblyOption).value.copy(includeScala = false)
