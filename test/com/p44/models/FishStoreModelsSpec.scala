package com.p44.models

import org.specs2.mutable.Specification

object FishStoreModelsSpec extends Specification {

  //sbt > test-only com.p44.models.FishStoreModelsSpec

  "FishStoreModels" should {
    "generateFishImpl" in {
      val fish: List[Fish] = FishStoreModels.generateFishImpl(10)
      println("generateFishImpl(10): " + fish)
      fish.size mustEqual 10
      
      val fish2: List[Fish] = FishStoreModels.generateFishImpl(1000)
      println("generateFishImpl(1000).size: " + fish2.size)
      fish2.size mustEqual 1000
    }
  }
}