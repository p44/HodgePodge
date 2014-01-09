package com.p44.actors

import org.specs2.mutable.Specification

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

import akka.actor.ActorSelection
import akka.actor.ActorSystem
import akka.testkit.TestProbe
import akka.actor.EmptyLocalActorRef
import scala.concurrent.Await
import akka.actor.Identify
import akka.actor.ActorRef
import akka.actor.ActorIdentity
import akka.actor.ActorSelection
import akka.actor.ActorSystem
import akka.actor.Identify
import akka.actor.Props
import akka.pattern.AskableActorSelection
import akka.pattern.ask
import akka.util.Timeout

object ToggleActorSpec extends Specification {

  //sbt > test-only com.p44.actors.ToggleActorSpec

  sequential

  implicit val system = ActorSystem("TestSys")
  val probe = TestProbe()
  val timeoutDefault = Duration(5, SECONDS)
  implicit val timeout = Timeout(timeoutDefault)

  "ToggleActor" should {
    "toggle" in {
      val toggle = system.actorOf(ToggleActor.propsToggle, "my_toggler")
      probe.send(toggle, ToggleActor.question)
      val e1: String = probe.expectMsg("cold")
      probe.send(toggle, ToggleActor.question)
      val e2: String = probe.expectMsg("hot")
      println("e1 and e2: " + e1 + " " + e2)
      e1 mustEqual "cold"
      e2 mustEqual "hot"

      probe.send(toggle, "Not a question")
      val e3: String = probe.expectMsg("unknown")
      system.stop(toggle)
      e3 mustEqual "unknown"
    }

    "lookup toggle" in {
      val toggle = system.actorOf(ToggleActor.propsToggle, "my_toggler2")
      println("lookup toggle - toggle.path " + toggle.path) // akka://TestSys/user/my_toggler
      println("lookup toggle - toggle.path.name " + toggle.path.name)
      toggle.path.name mustEqual "my_toggler2"
      
      // actorFor deprecated due to inconsistencies between local actor references and remote actor references
      val actorForRef = system.actorFor(toggle.path) 
      actorForRef mustEqual toggle 
      
      // actorSelection - 2.2 and up, works the same for local and remote actors
      val selToggler2: ActorSelection = system.actorSelection("/user/my_toggler2")
      val f: Future[Any] = selToggler2 ? Identify(None) // ask for identity
      val identityFuture: Future[Option[ActorRef]] = f.mapTo[ActorIdentity].map(_.ref)
      val oSelectedRef: Option[ActorRef] = Await.result(identityFuture, timeoutDefault) // Note: don't Await in an actor
      oSelectedRef.isDefined mustEqual true
      val selectedRef = oSelectedRef.get
      selectedRef mustEqual actorForRef // same reference
      
      // the selectedRef should work
      probe.send(selectedRef, ToggleActor.question)
      val e1: String = probe.expectMsg("cold")
      e1 mustEqual "cold"
      
      system.stop(toggle)
      selectedRef mustEqual toggle
    }
  }

  step {
    system.shutdown
    system.awaitTermination
  }

}