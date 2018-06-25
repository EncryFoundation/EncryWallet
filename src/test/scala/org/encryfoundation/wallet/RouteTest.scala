package org.encryfoundation.wallet

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{Matchers, PropSpec}

class RouteTest extends PropSpec with Matchers with ScalatestRouteTest  {

  val testSecret: String = "ACXe3zNvbM8magpUg3s4wBPppGULLxsBdmbnSwH2RzCG"

  property("settings") {
    Get(s"/settings?privateKey=$testSecret") ~> WebServer.route ~> check {
      response.status == StatusCodes.OK
    }
  }
}