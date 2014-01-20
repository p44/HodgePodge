package com.p44.actors

import akka.actor.Actor
import akka.actor.Props
import akka.actor.actorRef2Scala

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