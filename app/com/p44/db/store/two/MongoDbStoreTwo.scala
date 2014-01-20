package com.p44.db.store.two

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

import com.p44.models.FishStoreModels

import reactivemongo.api.DefaultDB
import reactivemongo.api.MongoDriver
import reactivemongo.api.MongoConnection
import reactivemongo.api.collections.default.BSONCollection

object MongoDbStoreTwo {

  val driver: MongoDriver = new MongoDriver
  val timeoutDefault: FiniteDuration = 5 seconds

  lazy val connFishStoreTwoDb: MongoConnection = driver.connection(FishStoreModels.FISHSTORE_TWO_DB_HOSTS) 
  lazy val STORE_TWO_DB: DefaultDB = connFishStoreTwoDb.db(FishStoreModels.FISHSTORE_TWO_DB_NAME) 
  
  def getCollection(db: DefaultDB, collName: String): BSONCollection = { 
    db.collection(collName) // ignore failover strategy for simplicity 
  }
  
  val collectionNameFishDelivered = "fish_delivered"
  lazy val collectionFishDelivered = getCollection(STORE_TWO_DB, collectionNameFishDelivered)
  
}