package org.encryfoundation.wallet

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.util.ByteString
import io.circe._
import org.encryfoundation.wallet.crypto.PrivateKey25519
import org.encryfoundation.wallet.transaction.{EncryTransaction, Transaction}
import org.encryfoundation.wallet.transaction.box.{AssetBox, EncryBox}

import scala.concurrent._
import scala.io.StdIn
import scala.util.Success

object WebServer {
  def main(args: Array[String]): Unit = {

    implicit val system: ActorSystem = ActorSystem("my-system")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    import org.encryfoundation.wallet.Page._
    import org.encryfoundation.wallet.utils.ExtUtils._

    val (pr, pub) = PrivateKey25519.generateKeys("1".getBytes)
    val walletData = new WalletData( pr, pub,
      PrivateKey25519.generateKeys("2".getBytes)._2.address
    )

    import io.circe.syntax._


    val route =
      path(""){
        get {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, page(jumbModalButton, modal).render))
        }

      } ~ path("test") {
        parameters('recepient.as[String], 'fee.as[Long], 'change.as[Long], 'amount.as[Long]) { (recepient, fee, change, amount) =>
          val nodeUri = Uri(s"http://172.16.10.55:9051/account/${walletData.user1PublicKey}/boxes").trace
          val (prKey, pubKey) = PrivateKey25519.generateKeys("1".getBytes)

          val useboxes: Future[Either[_,IndexedSeq[AssetBox]]] = Http().singleRequest(
            HttpRequest(uri = nodeUri)
          ) .flatMap(_.entity.dataBytes.runFold(ByteString.empty)(_ ++ _))
            .map(_.utf8String.trace)
            //            .map(decode[Seq[AssetBox]])
            .map(_ => Right(Seq.empty[AssetBox]))
            .map(_.map(_.foldLeft(Seq[AssetBox]()) { case (seq, box) =>
              if (seq.map(_.value).sum < (amount + fee)) seq :+ box else seq
            }.toIndexedSeq
            )
            )
          onComplete(useboxes){
            case Success(Right(boxes))  =>
              val transaction: EncryTransaction =
                Transaction.defaultPaymentTransactionScratch(
                  prKey, fee, System.currentTimeMillis, boxes, recepient, amount, None)
              transaction.asJson.trace
              complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, walletData.view.render))
            case x => complete(StatusCodes.OK).trace(x)
          }

        }
      } ~ path("test" ) {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, walletData.view.render))
      }

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

      println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}