package controllers

import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.libs.json._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{ Failure, Success }

object SimpleController extends Controller {

  lazy val homeMsg = "This is Hodge Podge"

  def index = Action.async {
	Future.successful(Ok(homeMsg))
  }

}
