package controllers

import play.api.libs.concurrent.Akka
import play.api.Play.current
import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.libs.json._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.mvc.Request
import play.api.mvc.AnyContent

import views._

/**
 * Restful services for Fish Store One
 */
object FishStoreOneController extends Controller {

  import com.p44.actors.store.one.FishStoreOne
  import com.p44.models.{FishStoreModels, Fish}

  // one reference to the controller actor
  val controllerActor = Akka.system.actorOf(FishStoreOne.propsController, name = "fishStoreOneController")
  lazy val msgDeliveryReceivedJson = """{"message": "Delivery Received"}"""
  lazy val defaultCatchSize = 10
  
  /** route to home page */
  def viewStoreOne = Action.async { request =>
	Future { Ok(views.html.fishstoreone.render) }
  }

  /**
   * GET /store_one/catch/latest
   * Provides a new load of fish as json array (simulated)
   */
  def getCatchLatest = Action.async {
    // functions returning futures
    val f: Future[String] = FishStoreModels.aBunchOfFishToJson(FishStoreModels.generateFish(defaultCatchSize))
    f.map(s => Ok(s)) // Note: f.onComplete does not work here because it returns Unit
  }
  
  /**
   * POST /store_one/delivery
   * Takes a shipment of fish into the store.
   */
  def postDelivery = Action.async { request =>
    val fDelivery: Future[Option[List[Fish]]] = Future {
      resolveDeliveryJsonToObj(request)
    }
    // futures and callbacks within other futures, introducing flatMap
    fDelivery.flatMap { delivery: Option[List[Fish]] =>
      delivery.isDefined match {
        case false => Future.successful(BadRequest("Please check your request for content type of json as well as the json format."))
        case _ => {
          // finally an akka actor call.  This is a tell, an asynchronous message send to an actor
          controllerActor ! FishStoreOne.Deliver(delivery.get) // fire and forget, no return
          val mt: Future[String] = FishStoreModels.makeMessageTimeJson("Delivery Received", System.currentTimeMillis())
          mt.map(s => Ok(s)) // because this map is here, flatMap must wrap otherwise it is Future[Future[SimpleResult]]
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
  } // no futures in this code, but the caller wraps in a Future {} block
  

}