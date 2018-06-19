import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpHeader.ParsingResult.Ok
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.util.ByteString
import encry.account.{Account, Address}
import encry.crypto.{PrivateKey25519, PublicKey25519}
import encry.modifiers.mempool.{EncryTransaction, TransactionFactory}
import encry.modifiers.state.box.{AssetBox, MonetaryBox}

import scala.concurrent.Future
import scala.util.Success
//import encry.
import io.circe._, io.circe.parser._

import scala.io.StdIn

object WebServer {
  def main(args: Array[String]) {

    implicit val system = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    import Page._
    import ExtUtils._

    val (pr, pub) = PrivateKey25519.generateKeys("1".getBytes)
    val walletData = new WalletData( pr, pub,
      PrivateKey25519.generateKeys("2".getBytes)._2.address
    )

    import encry.modifiers.mempool.EncryTransaction._
    import io.circe.syntax._
    import encry.modifiers

    val route =
      path(""){
        get {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, page(jumbModalButton, modal).render))
        }

      } ~ path("test") {
        parameters('recepient, 'fee.as[Long], 'change.as[Long], 'amount.as[Long]) { (recepient, fee, change, amount) =>
          val nodeUri = Uri(s"http://172.16.10.55:9051/account/${walletData.user1PublicKey}/boxes").trace
          val (prKey, pubKey) = PrivateKey25519.generateKeys("1".getBytes)
          val useboxes: Future[Either[_,IndexedSeq[MonetaryBox]]] = Http().singleRequest(
            HttpRequest(uri = nodeUri)
          ) .flatMap(_.entity.dataBytes.runFold(ByteString.empty)(_ ++ _))
            .map(_.utf8String.trace)
//            .map(decode[Seq[MonetaryBox]])
            .map(_ => Right(Seq.empty[MonetaryBox]))
            .map(_.map(_.collect{
                  case x: AssetBox => x
                }.foldLeft(Seq[AssetBox]()) { case (seq, box) =>
                  if (seq.map(_.amount).sum < (amount + fee)) seq :+ box else seq
                }.toIndexedSeq
              )
            )
          onComplete(useboxes){
            case Success(Right(boxes))  =>
              val transaction: EncryTransaction =
                TransactionFactory.defaultPaymentTransactionScratch(
                  prKey, fee, System.currentTimeMillis, boxes, recepient.asInstanceOf[Address], amount, None)
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