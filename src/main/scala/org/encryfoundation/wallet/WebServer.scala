package org.encryfoundation.wallet

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.util.ByteString
import org.encryfoundation.prismlang.lib.predefined.decode.Base58decode
import org.encryfoundation.wallet.crypto.PrivateKey25519
import org.encryfoundation.wallet.transaction.box.AssetBox
import org.encryfoundation.wallet.transaction.{EncryTransaction, Transaction}
import scorex.crypto.encode.Base58
import scorex.crypto.signatures.PrivateKey

import scala.concurrent._
import scala.io.StdIn
import scala.util.{Failure, Success}

object WebServer {
  def main(args: Array[String]): Unit = {

    implicit val system: ActorSystem = ActorSystem("my-system")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    import org.encryfoundation.wallet.Page._
    import org.encryfoundation.wallet.utils.ExtUtils._

    val (_, pub) = PrivateKey25519.generateKeys("1".getBytes)
    var walletData = new WalletData( None, pub, "3BxEZq6XcBzMSoDbfmY1V9qoYCwu73G1JnJAToaYEhikQ3bBKK")

    import io.circe.syntax._


    val route =
      path("") {
        parameters('fee.as[Long], 'amount.as[Long],'recepient.as[String]) { (fee, amount, recepient) =>
          val useboxes = walletData.wallet.map(_.account.address).map(
            address => Uri(s"http://172.16.10.58:9051/account/$address/boxes")
          ).map( uri => Http().singleRequest(
            HttpRequest(uri = uri)
          ) .flatMap(_.entity.dataBytes.runFold(ByteString.empty)(_ ++ _))
            .map(_.utf8String.trace)
            //            .map(decode[Seq[AssetBox]])
            .map(_ => Right(Seq.empty[AssetBox]))
            .map(_.map(_.foldLeft(Seq[AssetBox]()) { case (seq, box) =>
              if (seq.map(_.value).sum < (amount + fee)) seq :+ box else seq
            }.toIndexedSeq
            )
            )
          ).getOrElse(Future.failed(new Exception("Empty wallet")))
          onComplete(useboxes){
            case Success(Right(boxes))  =>
              walletData.wallet.map(wallet =>
                Transaction.defaultPaymentTransactionScratch(
                  wallet.getSecret, fee, System.currentTimeMillis, boxes, recepient, amount, None)
                    .asJson.trace)
              complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, walletData.view.render))
            case x => complete(StatusCodes.OK).trace(x)
          }

        }
      } ~ {
        path("") {
          parameters('privateKey.as[String].?) { privateKey =>
            val wallet: Option[Wallet] = privateKey.flatMap(Base58.decode(_).toOption).map(x => Wallet.initWithKey(PrivateKey @@ x))
            if (wallet.isDefined) walletData = walletData.copy(wallet = wallet).trace
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, walletData.view.render))
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