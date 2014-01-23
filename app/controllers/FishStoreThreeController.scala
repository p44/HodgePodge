package controllers

import views._
import com.p44.broadcast.FishStoreBroadcaster

import akka.pattern.ask
import akka.util.Timeout
import play.api.Play.current
import play.api.mvc.{ Action, Controller }
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
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
object FishStoreThreeController extends Controller {
  
  import com.p44.actors.store.three.FishStoreThree
  import com.p44.models.{ DeliveryReceipt, Fish, FishStoreModels }

  // one reference to the controller actor
  val controllerActor = Akka.system.actorOf(FishStoreThree.propsController, name = "fishStoreThreeController")
  lazy val defaultCatchSize = 100
  implicit val timeout = Timeout(6.seconds) // used for ask ?
  
  /** route to home page */
  def viewStoreThree = Action.async { request =>
	Future { Ok(views.html.fishstorethree.render) }
  }

  /**
   * GET /store_three/catch/latest
   * Provides a new load of fish as json array (simulated)
   */
  def getCatchLatest(size: Int) = Action.async {
    val f: Future[String] = FishStoreModels.aBunchOfFishToJson(FishStoreModels.generateFish(size))
    f.map(s => Ok(s)) 
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
          //controllerActor ! FishStoreThree.Deliver(delivery.get)
          val f: Future[Any] = controllerActor ? FishStoreThree.Deliver(delivery.get) // deliver with ask
          val fdr: Future[DeliveryReceipt] = f.mapTo[DeliveryReceipt]
          fdr.map { dr: DeliveryReceipt =>
            Ok(Json.prettyPrint(Json.toJson(dr)))
          }
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

  // Feed
  
  /** Enumeratee for detecting disconnect of the stream */
  def connDeathWatch(addr: String): Enumeratee[JsValue, JsValue] = {
    Enumeratee.onIterateeDone { () =>
      Logger.info(addr + " - FishStoreThreeOut disconnected")
    }
  }
  
  /** Controller action serving activity for fish store two (no filter) */
  def fishStoreThreeDeliveryFeed = Action { req =>
    Logger.info("FEED FishStoreThree - " + req.remoteAddress + " - FishStoreThree connected")
    Ok.chunked(FishStoreBroadcaster.fishStoreThreeOut
      &> Concurrent.buffer(50)
      &> connDeathWatch(req.remoteAddress)
      &> EventSource()).as("text/event-stream")
  }

}