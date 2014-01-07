import sbt._
import Keys._
import play.Project._
 
object ApplicationBuild extends Build {
 
  val appName         = "HodgePodge"
  val appVersion      = "1.0"
 
  val appDependencies = Seq(
      "com.typesafe.akka" %% "akka-testkit" % "2.2.1" % "test",
      cache
  )
 
  val main = play.Project(appName, appVersion, appDependencies).settings(
    scalaVersion := "2.10.2"
  )
 
}