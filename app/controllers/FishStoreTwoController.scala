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
object FishStoreTwoController extends Controller {
  
  import com.p44.actors.store.two.FishStoreTwo
  import com.p44.models.{ DeliveryReceipt, Fish, FishStoreModels }

  // one reference to the controller actor
  val controllerActor = Akka.system.actorOf(FishStoreTwo.propsController, name = "fishStoreTwoController")
  lazy val defaultCatchSize = 100
  implicit val timeout = Timeout(6.seconds) // used for ask ?
  
  /** route to home page */
  def viewStoreTwo = Action.async { request =>
	Future { Ok(views.html.fishstoretwo.render) }
  }
  
  /** watch dropped fish */
  def viewStoreTwoWatch = Action.async { request =>
	Future { Ok(views.html.droppedfishwatch.render) }
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
          //controllerActor ! FishStoreTwo.Deliver(delivery.get)
          val f: Future[Any] = controllerActor ? FishStoreTwo.Deliver(delivery.get) // deliver with ask
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

  // Added...
  
  /** Enumeratee for detecting disconnect of the stream */
  def connDeathWatch(addr: String): Enumeratee[JsValue, JsValue] = {
    Enumeratee.onIterateeDone { () =>
      Logger.info(addr + " - fishStoreTwoOut disconnected")
    }
  }
  
  /** Controller action serving activity for fish store two (no filter) */
  def fishStoreTwoDeliveryFeed = Action { request =>
    Logger.info("FEED fishStoreTwo - " + request.remoteAddress + " - fishStoreTwo connected")
    // Enumerator: a producer of typed chunks of data (non-blocking producer)
    val enumerator: Enumerator[JsValue] = FishStoreBroadcaster.fishStoreTwoOut
    Ok.chunked(enumerator
      through Concurrent.buffer(100)  // buffers chunks and frees the enumerator to keep processing
      through connDeathWatch(request.remoteAddress)
      through EventSource()).as("text/event-stream")
  }
  
  /*
   * Chunked transfer encoding is a data transfer mechanism in version 1.1 of the Hypertext Transfer Protocol (HTTP) 
   * in which a web server serves content in a series of chunks. 
   * It uses the Transfer-Encoding HTTP response header instead of the Content-Length header
   * http://www.playframework.com/documentation/2.2.0/ScalaStream
   * http://en.wikipedia.org/wiki/Chunked_transfer_encoding
   */

}