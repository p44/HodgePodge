package controllers

import views._
import com.p44.broadcast.FishStoreBroadcaster


import play.api.Play.current
import play.api.mvc.{ Action, Controller }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.libs.concurrent.Akka
import play.api.libs.json._
import play.api.libs.iteratee.{ Concurrent, Enumeratee, Enumerator }
import play.api.libs.EventSource
import play.api.mvc.Request
import play.api.mvc.AnyContent
import play.api.Logger

/**
 * Restful services for Fish Store Two
 */
object FishStoreTwoController extends Controller {
  
  import com.p44.actors.store.two.FishStoreTwo
  import com.p44.models.{FishStoreModels, Fish}

  // one reference to the controller actor
  val controllerActor = Akka.system.actorOf(FishStoreTwo.propsController, name = "fishStoreTwoController")
  lazy val defaultCatchSize = 100
  
  /** route to home page */
  def viewStoreTwo = Action.async { request =>
	Future { Ok(views.html.fishstoretwo.render) }
  }

  /**
   * GET /store_one/catch/latest
   * Provides a new load of fish as json array (simulated)
   */
  def getCatchLatest = Action.async {
    val f: Future[String] = FishStoreModels.aBunchOfFishToJson(FishStoreModels.generateFish(defaultCatchSize))
    f.map(s => Ok(s)) // Note: f.onComplete does not work here because it returns Unit
  }
  
  /**
   * POST /store_one/delivery
   * Takes a shipment of fish into the store.
   */
  def postDelivery = Action.async { request =>
    val fDelivery = Future[Option[List[Fish]]] {
      resolveDeliveryJsonToObj(request)
    }
    fDelivery.flatMap { delivery: Option[List[Fish]] =>
      delivery.isDefined match {
        case false => Future.successful(BadRequest("Please check your request for content type of json as well as the json format."))
        case _ => {
          controllerActor ! FishStoreTwo.Deliver(delivery.get)
          val mt: Future[String] = FishStoreModels.makeMessageTimeJson("Delivery Received", System.currentTimeMillis())
          mt.map(s => Ok(s))
        }
      }
    }
  }

  
  /** Takes a delivery, currently a json array of fish and creates an object to pass to the actors */
  def resolveDeliveryJsonToObj(request: Request[AnyContent]): Option[List[Fish]] = {
    val jsonBody: Option[JsValue] = request.body.asJson
    jsonBody.isDefined match {
      case false => None
      case true => {
        Json.fromJson[List[Fish]](jsonBody.get).asOpt
      }
    }
  }

  // Added...
  
  /** Enumeratee for detecting disconnect of the stream */
  def connDeathWatch(addr: String): Enumeratee[JsValue, JsValue] = {
    Enumeratee.onIterateeDone { () =>
      Logger.info(addr + " - fishStoreTwoOut disconnected")
    }
  }
  
  /** Controller action serving activity for fish store two (no filter) */
  def fishStoreTwoDeliveryFeed = Action { req =>
    Logger.info("FEED fishStoreTwo - " + req.remoteAddress + " - fishStoreTwo connected")
    Ok.chunked(FishStoreBroadcaster.fishStoreTwoOut
      &> Concurrent.buffer(50)
      &> connDeathWatch(req.remoteAddress)
      &> EventSource()).as("text/event-stream")
  }

}