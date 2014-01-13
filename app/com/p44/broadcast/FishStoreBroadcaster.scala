package com.p44.broadcast

import play.api.libs.iteratee.{ Concurrent, Enumeratee, Enumerator }
import play.api.libs.json.JsValue

object FishStoreBroadcaster {

  /**
   * Hub for distributing Fish Store Two Messages
   *  fishStoreTwoOut: Enumerator[JsValue]
   *  fishStoreTwoChannel: Channel[JsValue]
   */
  val (fishStoreTwoOut, fishStoreTwoChannel) = Concurrent.broadcast[JsValue] 
  
}