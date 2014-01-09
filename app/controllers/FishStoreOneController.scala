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

/**
 * Restful requests for Fish Store One
 */
object FishStoreOneController extends Controller {

  import com.p44.actors.store.one.FishStoreOne
  import com.p44.models.{FishStoreModels, Fish}

  // one reference to the controller actor
  val controllerActor = Akka.system.actorOf(FishStoreOne.propsController, name = "fishStoreOneController")

  /**
   * GET /store_one/catch/latest
   * Provides a new load of fish as json array (simulated)
   */
  def getCatchLatest = Action.async {
    val f: Future[String] = FishStoreModels.aBunchOfFishToJson(FishStoreModels.generateFish)
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
    fDelivery.map { delivery: Option[List[Fish]] =>
      delivery.isDefined match {
        case false => BadRequest("Please check your request for content type and expected json.")
        case _ => {
          controllerActor ! FishStoreOne.Deliver(delivery.get)
          Ok("""{"message": "Delivery Received}""")
        }
      }
    }
  }

  
  /** Takes a deliver, currently just a json array of fish and creates an object to pass to the actors */
  def resolveDeliveryJsonToObj(request: Request[AnyContent]): Option[List[Fish]] = {
    val jsonBody: Option[JsValue] = request.body.asJson
    jsonBody.isDefined match {
      case false => None
      case true => {
        Json.fromJson[List[Fish]](jsonBody.get).asOpt
      }
    }
  }

}