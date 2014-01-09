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
import play.api.cache.Cache // http://ehcache.org/

/**
 * The fish store.  Receiving fish, unload, stack, sell, package.
 *
 * Shipment of Fish --> Controller --> Unloader --> Catcher + Stacker
 *
 * This example uses the Play default thread pool with default settings.
 */
object FishStoreOne {
  
  val UnknownMessage = "unknown"

  val propsController = Props[FishStoreController]
  val propsUnloader = Props[FishUnloader]
  val propsCatcher = Props[FishCatcher]
  val propsStacker = Props[FishStacker]

  //val controller = Akka.system.actorOf(propsController, name = "fishStoreController")

  // MESSAGES
  case object Echo
  case object Done
  case class Deliver(shipment: List[Fish])
  case class Unload(fish: Fish)
  case class Catch(fish: Fish) // catch and hand off to an available stacker
  case class Stack(fish: Fish) // possibly considers containers being full

}

case class Fish(name: String, weight: Double)

/** Controller */
class FishStoreController extends Actor with ActorLogging {
  
  def receive = {
    case FishStoreOne.Deliver(shipment) => {
      log.info("Delivery of this many fish: " + shipment.size)
      val unloader = context.actorOf(FishStoreOne.propsUnloader) // create one unloader for each delivery
      shipment.foreach { x => unloader ! FishStoreOne.Unload(x) }
    }
    case FishStoreOne.Done => print("d")
    case FishStoreOne.Echo => sender ! "Echo"
    case _ => sender ! FishStoreOne.UnknownMessage
  }
}

class FishUnloader extends Actor with ActorLogging {
  def receive = {
    case FishStoreOne.Unload(fish) => {
      log.debug("Unloaded " + fish)
      val catcher = context.actorOf(FishStoreOne.propsCatcher)
      catcher ! FishStoreOne.Catch(fish) // catcher will stop itself
      context.parent ! FishStoreOne.Done
    }
    case FishStoreOne.Done => print("_") 
    case _ => log.error("unknown case")
  }
  def stop() = {
    context.parent ! FishStoreOne.Done
    context.stop(self)
  }
}

class FishCatcher extends Actor with ActorLogging {
  def receive = {
    case FishStoreOne.Catch(fish) => {
      log.debug("Catch " + fish)
      val stacker = context.actorOf(FishStoreOne.propsStacker)
      stacker ! FishStoreOne.Stack(fish) // catcher will stop itself
      //context.system.actorSelection(path) // actorFor is deprecated in favor of actorSelection because actor references acquired with actorFor behave differently for local and remote actors. 
    }
    case FishStoreOne.Done => {
      print("|")
      stop()
    }
    case _ => log.error("unknown case")
  }
  def stop() = {
    context.parent ! FishStoreOne.Done
    context.stop(self)
  }
}

class FishStacker extends Actor with ActorLogging {
  def receive = {
    case FishStoreOne.Stack(fish) => {
      log.debug("Stack " + fish)
      packOnIce(fish)
      context.parent ! FishStoreOne.Done
    }
    case _ => log.error("unknown case")
  }
  def packOnIce(fish: Fish) = {
    log.info("Packed on Ice: " + fish)
  }
  def stop() = {
    //context.stop(self) 
    // the parent will stop and this child will stop as a consequence if here we get...
    // [akka://TestSys/user/$a/$a/$b/$a] Message [akka.dispatch.sysmsg.Terminate] from Actor[akka://TestSys/user/$a/$a/$b/$a#1933714031] 
    // to Actor[akka://TestSys/user/$a/$a/$b/$a#1933714031] was not delivered. [1] dead letters encountered. 
  }
}

// ISSUES
// 1. 
