package controllers

import play.api.libs.concurrent.Akka // just use the play default akka for this low volume client
import play.api.Play.current // bring the current running Application into context
import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.libs.json._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Restful requests for Fish Store One
 */
object FishStoreOneController extends Controller {

  import com.p44.actors.FishStoreOne
  import com.p44.models.{FishStoreModels, Fish}

  // one reference to the controller actor
  val controllerActor = Akka.system.actorOf(FishStoreOne.propsController, name = "fishStoreOneController")

  /**
   * GET /store_one/fish
   * Provides a new load of fish as json array (simulated)
   */
  def getFish = Action.async {
    val f: Future[String] = FishStoreModels.aBunchOfFishToJson(FishStoreModels.generateFish)
    f.map(s => Ok(s)) // Note: f.onComplete does not work here because it returns Unit
  }
  
  /**
   * POST /store_one/delivery
   * Takes a shipment of fish into the store.
   */
  def postDelivery = Action {
    NotImplemented("TBD")
  }


}