package com.p44.actors

import akka.actor._
import play.api.libs.concurrent.Akka // just use the play default akka for this low volume client
import play.api.Play.current // bring the current running Application into context
import play.api.Logger
import scala.collection.mutable._
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{ Failure, Success }

object ToggleActor {

  val question = "Hot or Cold?"
  val propsToggle = Props[ToggleActor]

}

class ToggleActor extends Actor {
  def cold: Receive = {
    case ToggleActor.question => {
      sender ! "cold"
      context.become(hot)
    }
    case _ => sender ! "unknown"
  }
  def hot: Receive = {
    case ToggleActor.question => {
      sender ! "hot"
      context.become(cold)
    }
    case _ => sender ! "unknown"
  }
  def receive = cold
}