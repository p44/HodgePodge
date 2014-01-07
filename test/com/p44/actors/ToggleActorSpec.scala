package com.p44.actors

import org.specs2.mutable.Specification
import akka.actor.ActorSystem
import akka.testkit.TestProbe

object ToggleActorSpec extends Specification {

  //sbt > test-only com.p44.actors.ToggleActorSpec
  
  "ToggleActor" should {
    "toggle" in {
      implicit val system = ActorSystem("TestSys")
      val toggle = system.actorOf(ToggleActor.propsToggle)
      val p = TestProbe()
      p.send(toggle, ToggleActor.question)
      val e1: String = p.expectMsg("cold")
      p.send(toggle, ToggleActor.question)
      val e2: String = p.expectMsg("hot")
      println("e1 and e2: " + e1 + " " + e2)
      e1 mustEqual "cold"
      e2 mustEqual "hot"
      
      p.send(toggle, "Not a question")
      val e3: String = p.expectMsg("unknown")
      e3 mustEqual "unknown"
    }
  }

}