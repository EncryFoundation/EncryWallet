package org.encryfoundation.wallet

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpEntity, _}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.StandardRoute
import akka.stream.ActorMaterializer
import akka.util.ByteString
import io.circe.parser._
import org.encryfoundation.wallet.crypto.PrivateKey25519
import org.encryfoundation.wallet.transaction.box.AssetBox
import org.encryfoundation.wallet.transaction.box.AssetBox._
import org.encryfoundation.wallet.transaction.{EncryTransaction, Transaction}
import scorex.crypto.authds.ADKey
import scorex.crypto.encode.Base58
import scorex.crypto.signatures.PrivateKey

import scala.collection.immutable
import scala.concurrent._
import scala.io.StdIn
import io.circe.syntax._


case class TransactionWrapper(privKey: PrivateKey25519,
                         fee: Long,
                         timestamp: Long,
                         recipient: String,
                         amount: Long,
                         tokenIdOpt: Option[ADKey] = None){
  def default: IndexedSeq[AssetBox] => EncryTransaction = useBoxes =>
    Transaction.defaultPaymentTransactionScratch( privKey, fee, timestamp, useBoxes, recipient, amount, tokenIdOpt)
  def withContract(src: String): IndexedSeq[AssetBox] => EncryTransaction = useBoxes =>
    Transaction.scriptedAssetTransactionScratch(privKey, fee, timestamp, useBoxes, ???, amount, tokenIdOpt)
}

object WebServer {
  def main(args: Array[String]): Unit = {

    implicit val system: ActorSystem = ActorSystem("my-system")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    import org.encryfoundation.wallet.utils.ExtUtils._

    val (_, pub) = PrivateKey25519.generateKeys("1".getBytes)

    var walletData = new WalletData( None, pub, "3BxEZq6XcBzMSoDbfmY1V9qoYCwu73G1JnJAToaYEhikQ3bBKK")

    import io.circe.syntax._

    val nodeHost = "http://172.16.10.58:9051"

    def getBoxesFromNode(address: String, amountAndFee: Long): Future[immutable.IndexedSeq[AssetBox]] =
      Uri(s"$nodeHost/account/$address/boxes")
        .rapply(uri => HttpRequest(uri = uri))
        .rapply(Http().singleRequest(_))
        .flatMap(_.entity.dataBytes.runFold(ByteString.empty)(_ ++ _))
        .map(_.utf8String)
        .map(decode[Seq[AssetBox]])
        .map(_.map(_.foldLeft(Seq[AssetBox]()) {
          case (seq, box) =>
            if (seq.map(_.value).sum < amountAndFee) seq :+ box else seq
        }.toIndexedSeq))
        .flatMap{
          case Right(x) => Future.successful(x)
          case Left(e) => Future.failed(e)
        }

    def sendTransactionsToNode(encryTransaction: EncryTransaction) =
      encryTransaction.asJson.toString
        .rapply(HttpEntity(ContentTypes.`text/html(UTF-8)`,_))
        .rapply(HttpRequest(method = HttpMethods.POST, uri = Uri(s"$nodeHost/transactions/send")).withEntity(_))
        .rapply(Http().singleRequest(_))

    def pageRoute: StandardRoute = complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, walletData.view.render))

    def sendTransaction (fee: Long, amount: Long, recepient: String): Future[HttpResponse] =
      walletData.wallet.map{ wallet =>
        getBoxesFromNode(wallet.account.address, fee + amount).flatMap{ boxes =>
          //new TransactionWrapper(wallet.getSecret, fee, System.currentTimeMillis, recepient, amount))
          Transaction.defaultPaymentTransactionScratch(wallet.getSecret, fee, System.currentTimeMillis, boxes, recepient, amount, None)
            .rapply(sendTransactionsToNode)
        }
      }.getOrElse(Future.failed(new Exception("Send transaction without wallet")))

    def sendTransactionScript (fee: Long, amount: Long, src: String): Future[HttpResponse] =
    walletData.wallet.map{ wallet =>
      getBoxesFromNode(wallet.account.address, fee + amount).flatMap{ boxes =>
        val compiled = ???
        Transaction.scriptedAssetTransactionScratch(wallet.getSecret, fee, System.currentTimeMillis, boxes, compiled, amount, None)
          .rapply(sendTransactionsToNode)
      }
    }.getOrElse(Future.failed(new Exception("Send transaction without wallet")))

    val route =
      path("send"/"address") {
        parameters('fee.as[Long], 'amount.as[Long], 'recepient.as[String]) { (fee,amount,recepient) =>
          onSuccess( sendTransaction(fee,amount,recepient))(_ => pageRoute)
        }
      } ~ path("send"/"contract") {
        parameters('fee.as[Long], 'amount.as[Long], 'recepient.as[String], 'src.as[String]) { (fee,amount,recepient, src) =>
          onSuccess( sendTransactionScript(fee,amount,src))(_ => pageRoute)
        }
      } ~ {
        path("settings") {
          parameters('privateKey.as[String].?) { privateKey =>
            val wallet: Option[Wallet] = privateKey.flatMap(Base58.decode(_).toOption).map(x => Wallet.initWithKey(PrivateKey @@ x))
            if (wallet.isDefined) walletData = walletData.copy(wallet = wallet).trace
            pageRoute
          }
        }
      }//~ complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, walletData.view.render))

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}