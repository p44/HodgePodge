package com.p44.db.store.two

import com.p44.models._
import play.api.libs.functional.syntax.functionalCanBuildApplicative
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import reactivemongo.core.commands._
import reactivemongo.api._
import reactivemongo.bson._
import reactivemongo.api.collections.default.BSONCollection
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

case class FishDelivered(deliveryId: Long, fishName: String, pounds: Double, ts: Long)
object FishDeliveredDbHelper {
  val fieldDeliveryId = "deliveryId"
  val fieldFishName = "fishName"
  val fieldPounds = "pounds"
  val fieldTs = "ts"

  implicit object bsonWriter extends BSONDocumentWriter[FishDelivered] {
    def write(obj: FishDelivered): BSONDocument = toBson(obj: FishDelivered)
  }
  implicit object bsonReader extends BSONDocumentReader[FishDelivered] {
    def read(doc: BSONDocument): FishDelivered = fromBsonFromGoodDoc(doc)
  }

  val theBsonWriter: BSONDocumentWriter[FishDelivered] = bsonWriter
  val theBsonReader: BSONDocumentReader[FishDelivered] = bsonReader

  def toBson(obj: FishDelivered): BSONDocument = {
    BSONDocument(
      fieldDeliveryId -> obj.deliveryId,
      fieldFishName -> obj.fishName,
      fieldPounds -> obj.pounds,
      fieldTs -> obj.ts)
  }
  def fromBsonFromGoodDoc(doc: BSONDocument): FishDelivered = {
    FishDelivered(doc.getAs[Long](fieldDeliveryId).get,
      doc.getAs[String](fieldFishName).get,
      doc.getAs[Double](fieldPounds).get,
      doc.getAs[Long](fieldTs).get)
  }

  def insertOneAsFuture(obj: FishDelivered): Future[LastError] = {
    implicit val writer = theBsonWriter
    MongoDbStoreTwo.collectionFishDelivered.insert(obj)
  }

}