package org.encryfoundation.wallet

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpEntity, _}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Route, StandardRoute}
import akka.stream.ActorMaterializer
import akka.util.ByteString
import io.circe.parser._
import io.circe.syntax._
import org.encryfoundation.prismlang.compiler.CompiledContract
import org.encryfoundation.wallet.transaction.box.AssetBox
import org.encryfoundation.wallet.transaction.box.AssetBox._
import org.encryfoundation.wallet.transaction.{EncryTransaction, Transaction}
import org.encryfoundation.wallet.utils.ExtUtils._
import scorex.crypto.encode.Base58
import scorex.crypto.signatures.PrivateKey

import scala.collection.immutable
import scala.concurrent._
import scala.io.StdIn

object WebServer {

  implicit val system: ActorSystem = ActorSystem("my-system")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  var walletData: WalletData = WalletData(None, "3BxEZq6XcBzMSoDbfmY1V9qoYCwu73G1JnJAToaYEhikQ3bBKK")
  val nodeHost: String = "http://172.16.10.58:9051"

  def getBoxesFromNode(address: String, amountAndFee: Long): Future[immutable.IndexedSeq[AssetBox]] =
    Uri(s"$nodeHost/state/boxes/$address".trace)
      //.trace("Address").trace(address)
      .rapply(uri => HttpRequest(uri = uri))
      .rapply(Http().singleRequest(_))
      .flatMap(_.entity.dataBytes.runFold(ByteString.empty)(_ ++ _))
      .map(_.utf8String)
      .map(decode[Seq[AssetBox]])
      .map(_.map(_.foldLeft(Seq[AssetBox]()) { case (seq, box) =>
        if (seq.map(_.value).sum < amountAndFee) seq :+ box else seq
      }.toIndexedSeq))
      .flatMap {
        case Right(x) => Future.successful(x)
        case Left(e) => Future.failed(e.trace("Cant get boxes."))
      }

  def sendTransactionsToNode(encryTransaction: EncryTransaction): Future[HttpResponse] =
    encryTransaction.asJson.toString
      .trace("Send2Node")
      .rapply(HttpEntity(ContentTypes.`application/json`,_))
      .rapply(HttpRequest(method = HttpMethods.POST, uri = Uri(s"$nodeHost/transactions/send")).withEntity(_))
      .rapply(Http().singleRequest(_))

  def sendTransaction (fee: Long, amount: Long, recipient: String): Future[HttpResponse] =
    walletData.wallet.map { wallet =>
      getBoxesFromNode(wallet.account.address, fee + amount).flatMap { boxes =>
        Transaction.defaultPaymentTransactionScratch(wallet.getSecret, fee, System.currentTimeMillis, boxes, recipient, amount, None)
          .rapply(sendTransactionsToNode)
      }
    }.getOrElse(Future.failed(new Exception("Send transaction without wallet")))

  def sendTransactionScript (fee: Long, amount: Long, src: String): Future[HttpResponse] =
    walletData.wallet.map { wallet =>
      getBoxesFromNode(wallet.account.address, fee + amount).flatMap { boxes =>
        val compiled: CompiledContract = org.encryfoundation.prismlang.compiler.PCompiler.compile(src).get
        Transaction.scriptedAssetTransactionScratch(wallet.getSecret, fee, System.currentTimeMillis, boxes, compiled, amount, None)
          .rapply(sendTransactionsToNode)
      }
    }.getOrElse(Future.failed(new Exception("Send transaction without wallet")))

  def mainView: StandardRoute = complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, walletData.view.render))

  def mainR: Route = path("") { mainView }

  def sendPaymentTransactionR: Route = path("send"/"address") {
    parameters('fee.as[Long], 'amount.as[Long], 'recepient.as[String]) { (fee,amount,recepient) =>
      onSuccess( sendTransaction(fee,amount,recepient))(_ => mainView)
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

  val route: Route = sendPaymentTransactionR ~ sendScriptedTransactionR ~ walletSettingsR ~ mainR

  def main(args: Array[String]): Unit = {
    val bindingFuture: Future[Http.ServerBinding] = Http().bindAndHandle(route, "localhost", 8080)
    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}