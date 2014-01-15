package com.p44.actors.store.two

import org.specs2.mutable.Specification
import akka.actor.ActorSystem
import akka.testkit.TestProbe
import play.api.test._
import play.api.test.Helpers._
import com.p44.models._

object FishStoreTwoSpec extends Specification {

  //sbt > test-only com.p44.actors.store.two.FishStoreTwoSpec

  sequential

  val delivery = List(Fish("trout", 2.5), Fish("trout", 2.6), Fish("mackerel", 3.5))

  "FishStoreOne" should {
    "Deliver" in {
      running(FakeApplication()) {
        implicit val system = ActorSystem("TestSys")
        val controller = system.actorOf(FishStoreTwo.propsController)
        val p = TestProbe()
        p.send(controller, FishStoreTwo.Echo)
        val e1: String = p.expectMsg("Echo")
        e1 mustEqual "Echo"
        
        p.send(controller, FishStoreTwo.Deliver(delivery))
        Thread.sleep(1000L)
        system.shutdown
        system.awaitTermination
        true
      }
    }
  }

}
