package com.p44.models

import play.api.libs.json._
import play.api.libs.functional._
import play.api.libs.functional.syntax._
import play.api.data.validation.ValidationError
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class Fish(name: String, pounds: Double)
object Fish {
  implicit val jsonWriter = Json.writes[Fish] // Json.toJson(obj): JsValue
  implicit val jsonReader = Json.reads[Fish] // Json.fromJson[T](jsval): JsResult[T] .asOpt Option[T]
  def toJsArray(objs: List[Fish]): JsArray = JsArray(objs.map(Json.toJson(_)).toSeq)
}

object FishStoreModels {
  def aBunchOfFishToJson(futureShipment: Future[List[Fish]]): Future[String] = {
    futureShipment.map { shipment =>
      Json.prettyPrint(Fish.toJsArray(shipment))
    }
  }
  def generateFish: Future[List[Fish]] = Future { 
    List(Fish("mackerel", 3.5))
  }

}