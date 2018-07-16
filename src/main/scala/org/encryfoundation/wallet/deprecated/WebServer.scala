package org.encryfoundation.wallet.deprecated

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpEntity, _}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Route, StandardRoute}
import org.encryfoundation.wallet.Wallet
import org.encryfoundation.wallet.deprecated.Implicits._

import scala.concurrent._
import scala.io.StdIn

object WebServer extends WalletActions {

  def mainView: StandardRoute = complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, walletData.view2.render))

  def mainR: Route = path("") { mainView }

  def sendPaymentTransactionR: Route = path("send"/"address") {
    parameters('fee.as[Long], 'amount.as[Long], 'recipient.as[String]) { (fee,amount,recipient) =>
      onSuccess( walletWithError(sendTransaction(fee,amount,recipient)))(mainView)
    }
  }

  def sendPaymentTransactionWithBoxR: Route = path("send"/"withbox") {
    parameters('fee.as[Long], 'amount.as[Long], 'recipient.as[String], 'boxId.as[String], 'change.as[Long]) {
      (fee,amount,recipient, boxId, change) =>
        onSuccess(walletWithError(sendTransactionWithBox(fee,amount,recipient, boxId, change)))(mainView)
    }
  }

  def sendScriptedTransactionR: Route = path("send"/"contract") {
    parameters('fee.as[Long], 'amount.as[Long], 'src.as[String]) { (fee, amount, src) =>
      onSuccess(walletWithError(sendTransactionWithScript(fee, amount, src)))(mainView)
    }
  }

  def walletSettingsR: Route = path("settings") {
    parameters('privateKey.as[String].?) { privateKey =>
      walletData = walletData.copy(error = None)
      val wallet: Option[Wallet] = privateKey.flatMap(org.encryfoundation.wallet.actions.WalletActions.restoreFromSecret)
      if (wallet.isDefined) walletData = walletData.copy(wallet = wallet)
      mainView
    }
  }

  val route: Route = sendPaymentTransactionR ~ sendScriptedTransactionR ~ walletSettingsR ~ sendPaymentTransactionWithBoxR ~ mainR

  def main(args: Array[String]): Unit = {
    val bindingFuture: Future[Http.ServerBinding] = Http().bindAndHandle(route, "localhost", 8080)
    StdIn.readLine()
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }
}