package org.encryfoundation.wallet.http.api

import akka.actor.ActorRefFactory
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import io.circe.syntax._

case class WalletApiRoute(implicit val context: ActorRefFactory) extends ApiRoute {

  import org.encryfoundation.wallet.actions.WalletActions._

  override def route: Route = pathPrefix("wallet") {
    createNewWalletR ~ restoreWalletFromSecretR ~ getAllWalletsR ~ getAllWalletsInfoR
  }

  def createNewWalletR: Route = pathPrefix("create") {
    post(entity(as[String]) { seed =>
      complete {
        createNewWallet(seed)
        StatusCodes.OK
      }
    })
  }

  def restoreWalletFromSecretR: Route = pathPrefix("restore") {
    post(entity(as[String]) { secret =>
      complete {
        if (restoreFromSecret(secret).isDefined) StatusCodes.OK else StatusCodes.BadRequest
      }
    })
  }

  def getAllWalletsR: Route = (pathPrefix("all") & get) {
    toJsonResponse(loadAll.asJson)
  }

  def getAllWalletsInfoR: Route = (pathPrefix("all" / "info") & get) {
    toJsonResponse(loadAllWithExtendedInfo.map(_.asJson))
  }
}
