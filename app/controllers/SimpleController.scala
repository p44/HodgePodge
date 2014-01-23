package controllers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import play.api.mvc.Action
import play.api.mvc.Controller

/**
 * Index page, hello page and other simple returns
 */
object SimpleController extends Controller {

  lazy val homeMsg: String = "This is Hodge Podge"
  lazy val helloMsg: String = "Hello There" 

  def index = Action { // request => resopnse
	Ok(homeMsg) // 200
  }
  
  def hello(name: String) = Action.async { // downstream callback for the future
	Future {
	  Ok(helloMsg + ", " + name + ". " + homeMsg)
	}
  }
  
  // Play allows the return of a Future Result

}
