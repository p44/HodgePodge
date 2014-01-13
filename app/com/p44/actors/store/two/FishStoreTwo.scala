package com.p44.actors.store.two

import com.p44.models.Fish
import com.p44.models.DroppedFish

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props
import akka.actor.actorRef2Scala
import com.p44.broadcast.FishStoreBroadcaster
import play.api.libs.json._

object FishStoreTwo {
  
  val UnknownMessage = "unknown"

  val propsController = Props[FishStoreController]
  val propsUnloader = Props[FishUnloader]
  val propsCatcher = Props[FishCatcher]
  val propsStacker = Props[FishStacker]
  val propsCommentator = Props[FishCommentator]
  
  val commentatorName = "commentatorTwo"

  // MESSAGES
  case object Echo
  case object Done
  case class Deliver(shipment: List[Fish])
  case class Unload(fish: Fish)
  case class Catch(fish: Fish) // catch and hand off to an available stacker
  case class Stack(fish: Fish) // possibly considers containers being full
  case class AnnounceDroppedFish(droppedFish: DroppedFish)

  lazy val possibleExclamations = Seq("Darn it!", "I thought I had it!", "No no no!", "Sippery one!", "I wasn't looking.", "Nuts!", "Aaaaaa!", "Dammit!")

}

/** Controller */
class FishStoreController extends Actor with ActorLogging {
  
  // one dedicated unloader actor (handles all unload messages queued in order)
  val unloaderRef = context.actorOf(FishStoreTwo.propsUnloader)
  
  def receive = {
    case FishStoreTwo.Deliver(shipment) => {
      log.info("New delivery of this many fish: " + shipment.size)
      shipment.foreach { x => unloaderRef ! FishStoreTwo.Unload(x) }
    }
    case FishStoreTwo.Done => print("d")
    case FishStoreTwo.Echo => sender ! "Echo"
    case _ => sender ! FishStoreTwo.UnknownMessage
  }
}

// val uloaderSel = context.actorSelection(unloaderRef.path) // reuse the unloader for each delivery

/** Unload */
class FishUnloader extends Actor with ActorLogging {
  
  // one dedicate commentator broadcasting messages about the workings of the store
  val commentatorRef = context.actorOf(FishStoreTwo.propsCommentator)
  
  def receive = {
    case FishStoreTwo.Unload(fish) => {
      log.debug("Unloaded " + fish)
      val catcher = context.actorOf(FishStoreTwo.propsCatcher)
      catcher ! FishStoreTwo.Catch(fish) // catcher will stop itself
      context.parent ! FishStoreTwo.Done
    }
    case FishStoreTwo.AnnounceDroppedFish(droppedFish) => {
      commentatorRef ! FishStoreTwo.AnnounceDroppedFish(droppedFish)
    }
    case FishStoreTwo.Done => print("_")
    case _ => log.error("unknown case")
  }
  def stop() = {
    context.parent ! FishStoreTwo.Done
    context.stop(self)
  }
}

/** Catch */
class FishCatcher extends Actor with ActorLogging {
  
  val stacker = context.actorOf(FishStoreTwo.propsStacker) // create new stacker
  
  def receive = {
    case FishStoreTwo.Catch(fish) => {
      log.debug("Catch " + fish)
      val f: Option[Fish] = catchFish(fish) // Chance of dropped fish
      f.isDefined match {
        case false => { // dropped the fish
          context.parent ! FishStoreTwo.AnnounceDroppedFish(DroppedFish(fish, exclaim))
          self ! FishStoreTwo.Catch(fish) // pick up dropped fish (chance of drop same as catch)
        }
        case _ => {
          stacker ! FishStoreTwo.Stack(fish)
        }
      }
    }
    case FishStoreTwo.Done => {
      print("|")
      stop()
    }
    case _ => log.error("unknown case")
  }
  def stop() = {
    context.parent ! FishStoreTwo.Done
    context.stop(self)
  }
  /** catch the fish, there is a chance of drop */
  def catchFish(wigglyFish: Fish): Option[Fish] = {
    (math.random < 0.15) match {
      case true => {
        log.info("Dropped fish: " + wigglyFish)
        None
      }
      case _ => Some(wigglyFish)
    }
  }
  /** say something when dropping a fish */
  def exclaim: String = { 
    val size = FishStoreTwo.possibleExclamations.size
    FishStoreTwo.possibleExclamations(scala.util.Random.nextInt(size)) 
  }
}

/** Stack */
class FishStacker extends Actor with ActorLogging {
  def receive = {
    case FishStoreTwo.Stack(fish) => {
      log.debug("Stack " + fish)
      packOnIce(fish)
      context.parent ! FishStoreTwo.Done
    }
    case _ => log.error("unknown case")
  }
  def packOnIce(fish: Fish) = {
    log.info("Packed on Ice: " + fish)
  }
}

class FishCommentator extends Actor with ActorLogging {
  def receive = {
    case FishStoreTwo.AnnounceDroppedFish(droppedFish) => {
      val jsv: JsValue = Json.toJson(droppedFish)
      FishStoreBroadcaster.fishStoreTwoChannel.push(jsv) // dropped fish as json to the channel
    }
  }
}