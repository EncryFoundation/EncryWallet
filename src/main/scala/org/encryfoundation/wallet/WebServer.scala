package org.encryfoundation.wallet

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpEntity, _}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Route, StandardRoute}
import org.encryfoundation.wallet.Implicits._
import org.encryfoundation.wallet.utils.ExtUtils._
import scorex.crypto.encode.Base58
import scorex.crypto.signatures.PrivateKey

import scala.concurrent._
import scala.io.StdIn

object WebServer extends WalletActions {

  def mainView: StandardRoute = complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, walletData.view2.render))

  def mainR: Route = path("") { mainView }

  def sendPaymentTransactionR: Route = path("send"/"address") {
    parameters('fee.as[Long], 'amount.as[Long], 'recipient.as[String]) { (fee,amount,recipient) =>
      onSuccess( sendTransaction(fee,amount,recipient))(_ => mainView)
    }
  }

  def sendPaymentTransactionWithBoxR: Route = path("send"/"withbox") {
    parameters('fee.as[Long], 'amount.as[Long], 'recipient.as[String], 'boxId.as[String], 'change.as[Long]) {
      (fee,amount,recipient, boxId, change) =>
        onSuccess( sendTransactionWithBox(fee,amount,recipient, boxId, change))(_ => mainView)
    }
  }

  def sendScriptedTransactionR: Route = path("send"/"contract") {
    parameters('fee.as[Long], 'amount.as[Long], 'src.as[String]) { (fee, amount, src) =>
      onSuccess( sendTransactionScript(fee, amount, src))(_ => mainView)
    }
  }

  def walletSettingsR: Route = path("settings") {
    parameters('privateKey.as[String].?) { privateKey =>
      val wallet: Option[Wallet] = privateKey.trace.flatMap(Base58.decode(_).toOption)
        .map(x => Wallet.initWithKey(PrivateKey @@ x))
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