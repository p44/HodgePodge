package com.p44.actors.store.two

import com.p44.models._

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.pattern.{ ask, pipe }
import akka.util.Timeout
import com.p44.broadcast.FishStoreBroadcaster
import play.api.libs.json._
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{ Failure, Success }

/**
 * Changes and additions from One:
 * 1.  Actor reference management
 * 2.  Catcher now has a chance of drop fish with recovery
 * 3.  Catcher now demonstrates behavior state change with context.become
 * 4.  Added a Commentator that broadcasts the dropped fish events.
 */
object FishStoreTwo {

  val UnknownMessage = "unknown"

  val propsController = Props[FishStoreController]
  val propsDeliveryCalculator = Props[FishDeliveryCalculator]
  val propsUnloader = Props[FishUnloader]
  val propsCatcher = Props[FishCatcher]
  val propsStacker = Props[FishStacker]
  val propsCommentator = Props[FishCommentator]

  val commentatorName = "commentatorTwo"

  // MESSAGES
  case object Echo
  case object Done
  case class Deliver(shipment: List[Fish])
  case class GenerateReceipt(ts: Long, shipment: List[Fish]) // New functionality, returns DeliveryReceipt
  case class Unload(ts: Long, fish: Fish)
  case class Catch(ts: Long, fish: Fish) // catch and hand off to an available stacker
  case class Stack(ts: Long, fish: Fish) // possibly considers containers being full
  case class AnnounceDroppedFish(droppedFish: DroppedFish)

  lazy val possibleExclamations = Seq("Darn it!", "I thought I had it!", "No no no!", "Sippery one!", "I wasn't looking",
    "Nuts!", "Aaaaaa!", "Dammit!", "Are you kidding?", "Bad throw.", "Bollocks!", "Nunen", "That's weird", "Again?")

  /** Build the receipt */
  def calcReceipt(ts: Long, shipment: List[Fish]): DeliveryReceipt = {
    val totalWeight: Double = shipment.map(x => x.pounds).sum
    val time = FishStoreModels.formatTimstampMillis(ts, FishStoreModels.DATE_FORMATTER_USA)
    // DeliveryReceipt(id: Long, fishCount: Int, totalWeight: Double, payment: Double, time: String)
    DeliveryReceipt(ts, shipment.size, totalWeight, totalWeight * 3.0, time: String, "Thank You!")
  }
}

/** Controller */
class FishStoreController extends Actor with ActorLogging {

  // one dedicated unloader actor (handles all unload messages queued in order)
  val unloaderRef = context.actorOf(FishStoreTwo.propsUnloader)
  val calculatorRef = context.actorOf(FishStoreTwo.propsDeliveryCalculator)

  implicit val timeout = Timeout(5.seconds) // used for ask ?

  def receive = {
    case FishStoreTwo.Deliver(shipment) => {
      val now = System.currentTimeMillis
      log.info("New delivery of this many fish: " + shipment.size)
      shipment.foreach { x => unloaderRef ! FishStoreTwo.Unload(now, x) }
      // ask the calculator for a receipt, send the future back to sender
      (calculatorRef ? FishStoreTwo.GenerateReceipt(now, shipment)).map { a: Any => a } pipeTo sender
      //sender ! FishStoreTwo.calcReceipt(now, shipment) // alternative local calc in the actor
    }
    case FishStoreTwo.Done => print("d")
    case FishStoreTwo.Echo => sender ! "Echo"
    case _ => sender ! FishStoreTwo.UnknownMessage
  }

}

class FishDeliveryCalculator extends Actor with ActorLogging {
  def receive = {
    case FishStoreTwo.GenerateReceipt(ts, shipment) => {
      val dr = FishStoreTwo.calcReceipt(ts, shipment)
      log.info("GenerateReceipt: " + dr)
      sender ! dr // send it back or we timeout
    }
    case _ => sender ! FishStoreTwo.UnknownMessage
  }
}

// val uloaderSel = context.actorSelection(unloaderRef.path) // reuse the unloader for each delivery

/** Unload */
class FishUnloader extends Actor with ActorLogging {

  // one dedicate commentator broadcasting messages about the workings of the store
  val commentatorRef = context.actorOf(FishStoreTwo.propsCommentator)

  def receive = {
    case FishStoreTwo.Unload(ts, fish) => {
      log.debug("Unloaded " + fish)
      val catcher = context.actorOf(FishStoreTwo.propsCatcher)
      catcher ! FishStoreTwo.Catch(ts, fish) // catcher will stop itself
      context.parent ! FishStoreTwo.Done
    }
    case FishStoreTwo.AnnounceDroppedFish(droppedFish) => {
      commentatorRef ! FishStoreTwo.AnnounceDroppedFish(droppedFish)
    }
    case FishStoreTwo.Done => print("u")
    case _ => log.error("unknown case")
  }
}

/** Catch */
class FishCatcher extends Actor with ActorLogging {

  val stacker = context.actorOf(FishStoreTwo.propsStacker) // create new stacker

  def goodhands: Receive = {
    case FishStoreTwo.Catch(ts, fish) => {
      val f: Option[Fish] = grabFish(fish, 0.15) // Chance of dropped fish
      f.isDefined match {
        case false => { // dropped the fish
          context.parent ! FishStoreTwo.AnnounceDroppedFish(DroppedFish(fish, exclaim))
          context.become(butterfingers) // change to butterfingers state
          self ! FishStoreTwo.Catch(ts, fish) // pick up dropped fish (chance of drop same as catch)
        }
        case _ => { stacker ! FishStoreTwo.Stack(ts, fish) }
      }
    }
    case FishStoreTwo.Done => {
      print("c")
      stop()
    }
  }

  def butterfingers: Receive = {
    case FishStoreTwo.Catch(ts, fish) => {
      val f: Option[Fish] = grabFish(fish, 0.20)
      f.isDefined match {
        case false => { // dropped the fish
          context.parent ! FishStoreTwo.AnnounceDroppedFish(DroppedFish(fish, exclaim))
          self ! FishStoreTwo.Catch(ts, fish) // pick up dropped fish (chance of drop same as catch)
        }
        case _ => {
          stacker ! FishStoreTwo.Stack(ts, fish)
          context.become(goodhands) // change to butterfingers state
        }
      }
    }
    case FishStoreTwo.Done => {
      print("c")
      stop()
    }
  }

  def receive = goodhands

  def stop() = {
    context.parent ! FishStoreTwo.Done
    context.stop(self)
  }
  /** catch the fish, there is a chance of drop */
  def grabFish(wigglyFish: Fish, odds: Double): Option[Fish] = {
    (math.random < odds) match {
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
    case FishStoreTwo.Stack(ts, fish) => {
      log.debug("Stack " + fish)
      packOnIce(ts, fish)
      context.parent ! FishStoreTwo.Done
    }
  }
  
  import com.p44.db._ 
  
  def packOnIce(dId: Long, fish: Fish) = {
    val fd = com.p44.db.store.two.FishDelivered(dId, fish.name, fish.pounds, System.currentTimeMillis)
    val futureInsert = com.p44.db.store.two.FishDeliveredDbHelper.insertOneAsFuture(fd)
    futureInsert.onComplete {
      case Failure(e) => log.error("ERROR inserting deliverd fish: " + e.getMessage)
      case Success(le) => {
        le.ok match {
          case false => log.error("ERROR: Not Packed on Ice: " + fish)
          case _ => log.info("Packed on Ice: " + fish)
        }
      }
    }
  }
}

/** Broadcast dropped fish */
class FishCommentator extends Actor with ActorLogging {
  def receive = {
    case FishStoreTwo.AnnounceDroppedFish(droppedFish) => {
      val jsv: JsValue = Json.toJson(droppedFish)
      FishStoreBroadcaster.fishStoreTwoChannel.push(jsv) // dropped fish as json to the channel
    }
  }
}

// TODO
// 1.  The stacker still only does a println, lets have it persist
