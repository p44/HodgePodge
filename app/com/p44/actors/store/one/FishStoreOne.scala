package com.p44.actors.store.one

import com.p44.models.Fish

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props
import akka.actor.actorRef2Scala

/**
 * The fish store.
 * Receiving fish, unload, stack, sell, package.
 *
 * Delivery of Fish --> Controller --> Unloader --> Catcher + Stacker
 *
 * This example uses the Play default thread pool with default settings.
 */
object FishStoreOne {

  val UnknownMessage = "unknown"

  val propsController = Props[FishStoreController]
  val propsUnloader = Props[FishUnloader]
  val propsCatcher = Props[FishCatcher]
  val propsStacker = Props[FishStacker]

  // MESSAGES
  case object Echo
  case object Done
  case class Deliver(shipment: List[Fish])
  case class Unload(fish: Fish)
  case class Catch(fish: Fish) // catch and hand off to an available stacker
  case class Stack(fish: Fish) // possibly considers containers being full

}

/** Controller */
class FishStoreController extends Actor with ActorLogging {
  def receive = {
    case FishStoreOne.Deliver(shipment) => {
      log.info("New delivery of this many fish: " + shipment.size)
      // create one new unloader for each delivery
      val unloader = context.actorOf(FishStoreOne.propsUnloader)
      shipment.foreach { x => unloader ! FishStoreOne.Unload(x) }
    }
    case FishStoreOne.Done => print("u")
    case FishStoreOne.Echo => sender ! "Echo"
    case _ => sender ! FishStoreOne.UnknownMessage
  }
}

/** Unload */
class FishUnloader extends Actor with ActorLogging {
  def receive = {
    case FishStoreOne.Unload(fish) => {
      log.debug("Unloaded " + fish)
      val catcher = context.actorOf(FishStoreOne.propsCatcher)
      catcher ! FishStoreOne.Catch(fish) // catcher will stop itself
      context.parent ! FishStoreOne.Done
    }
    case FishStoreOne.Done => print("c")
    case _ => log.error("unknown case")
  }
  def stop() = {
    context.parent ! FishStoreOne.Done
    context.stop(self)
  }
}

/** Catch */
class FishCatcher extends Actor with ActorLogging {
  def receive = {
    case FishStoreOne.Catch(fish) => {
      log.debug("Catch " + fish)
      val stacker = context.actorOf(FishStoreOne.propsStacker) // create new stacker
      stacker ! FishStoreOne.Stack(fish)
    }
    case FishStoreOne.Done => {
      print("s")
      stop()
    }
    case _ => log.error("unknown case")
  }
  def stop() = {
    context.parent ! FishStoreOne.Done
    context.stop(self)
  }
}

/** Stack */
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
  // the parent will stop itself and this child will stop as a consequence 
  // if stacker stops itself here we get...
  // Message [akka.dispatch.sysmsg.Terminate] was not delivered. [1] dead letters encountered. 
}

// ISSUES - This uses actors but doesn't do much of interest
// 1. FishStoreOne.Deliver - The unloader is created but not stopped and not reused.  It could easily be reused
// 2. Catch and Stack don't have any significant behavior, results only in a log.info
// 3. It would be nice to send a delivery summary to the initiator - count and total weight.
// 4. Handling of dead letter messages?
