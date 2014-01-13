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

case class MessageTime(message: String, time: String)
object MessageTime {
  implicit val jsonWriter = Json.writes[MessageTime] // Json.toJson(obj): JsValue
  implicit val jsonReader = Json.reads[MessageTime] // Json.fromJson[T](jsval): JsResult[T] .asOpt Option[T]
  def toJsArray(objs: List[MessageTime]): JsArray = JsArray(objs.map(Json.toJson(_)).toSeq)
}

case class DroppedFish(fish: Fish, exclamation: String)
object DroppedFish {
  implicit val jsonWriter = Json.writes[DroppedFish] // Json.toJson(obj): JsValue
  implicit val jsonReader = Json.reads[DroppedFish] // Json.fromJson[T](jsval): JsResult[T] .asOpt Option[T]
  def toJsArray(objs: List[DroppedFish]): JsArray = JsArray(objs.map(Json.toJson(_)).toSeq)
}

object FishStoreModels {

  /** (name, min lbs, max lbs) */
  val possibleFish: Seq[(String, Int, Int)] = Seq(("sea bass", 5, 10),
    ("blue-green snapper", 4, 18),
    ("pink snapper", 2, 18),
    ("red snapper", 2, 18),
    ("wahoo", 8, 30),
    ("skipjack tuna", 4, 30))

  def getRandomFish: Fish = {
    val pf: (String, Int, Int) = getRandomPossibleFish
    getFishFromPossibleFish(pf)
  }
  def getRandomPossibleFish: (String, Int, Int) = {
    possibleFish(scala.util.Random.nextInt(possibleFish.size))
  }
  def getFishFromPossibleFish(pf: (String, Int, Int)): Fish = {
    Fish(pf._1, getFishWeight(pf._2, pf._3))
  }
  def getFishWeight(min: Int, max: Int): Double = {
    val lbs: Double = (min + scala.util.Random.nextInt(max - min + 1)).toDouble
    val frac: Double = (scala.util.Random.nextInt(10)).toDouble
    lbs + frac / 10.0
  }
  def aBunchOfFishToJson(futureShipment: Future[List[Fish]]): Future[String] = {
    futureShipment.map { shipment =>
      Json.prettyPrint(Fish.toJsArray(shipment))
    }
  }
  /** Generates a list of fish of size specified by count, returns as future */
  def generateFish(count: Int): Future[List[Fish]] = {
    Future { generateFishImpl(count) }
  }
  /** Generates a list of fish of size specified by count */
  def generateFishImpl(count: Int): List[Fish] = {
    val fish = for (i <- 0 until count) yield { getRandomFish }
    fish.toList
  }
  
  
  import org.joda.time.DateTime
  import org.joda.time.DateTimeZone
  import org.joda.time.format._
  
  val DATE_FORMATTER_USA: DateTimeFormatter = DateTimeFormat.forPattern("MM/dd/yyyy HH:mm:ss")
 
  def makeMessageTimeJson(msg: String, ts: Long): Future[String] = {
    Future { makeMessageTimeJsonImpl(msg, ts) }
  }
  def makeMessageTimeJsonImpl(msg: String, ts: Long): String = {
    val dt = new DateTime(ts)
    val formattedTimestamp: String = DATE_FORMATTER_USA.print(dt)
    val mt = MessageTime(msg, formattedTimestamp)
    Json.prettyPrint(Json.toJson(mt))
  }

}