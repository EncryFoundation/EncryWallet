package org.encryfoundation.wallet

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{Matchers, PropSpec}

class WalletTest extends PropSpec with Matchers with ScalatestRouteTest  {

  property("a") {
    Get()
  }

  val testSecret = "ACXe3zNvbM8magpUg3s4wBPppGULLxsBdmbnSwH2RzCG"

  property("settings") {
    Get(s"/settings?privateKey=$testSecret") ~> WebServer.route ~> check {
      response.status == StatusCodes.OK
    }
  }

//  property("sendTransaction"){
//    Get("/send/contract?fee=100&amount=10000&src=contract+%28sig%3A+Signature25519%2C+tx%3A+Transaction%2C+state%3A+State%29+%3D+checkSig%28sig%2C+tx.messageToSign%2C+base58%22GV1bs2cMY9FoPt3ZSXh73RLdtLKmfBCDfYjSFBXk1E7T%22%29") ~> WebServer.route ~> check {
//      response.status == StatusCodes.OK
//    }
//  }


  property("sendTransaction2"){
    Get("/send/contract?fee=100&amount=1000&src=contract+%28sig%3A+Signature25519%2C+tx%3A+Transaction%" +
      "2C+state%3A+State%29+%3D+state.height+%3E+50+%26%26+checkSig%28sig%2C+tx.messageToSign%2C+base58%" +
      "22Ci7gbDkpQegR2oq3BNJoEUAB6Peu4BhFgWB5qVTEBVxL%22%29%0D%0A")~> WebServer.route ~> check {
      response.status == StatusCodes.OK
    }
  }

//  property("jsonTransaction"){
//    Post("/send") ~> WebServer.route ~> check {
//      response.status == StatusCodes.OK
//    }
//  }
}