import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpHeader.ParsingResult.Ok
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import encry.account.Address
import encry.crypto.{PrivateKey25519, PublicKey25519}
import encry.modifiers.mempool.{EncryTransaction, TransactionFactory}
//import encry.

import scala.io.StdIn

object WebServer {
  def main(args: Array[String]) {

    implicit val system = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    import Page._
    import ExtUtils._
    val data = Map.empty[String,Number]
    val route =
      path(""){
        get {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, page(jumbModalButton, modal).render))
        }
//      } ~ path("send") {
//        get {
//          parameter('amount.as[Int], 'address.as[String] ){ (amount, address) =>
//            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, page(modalButton, modal).render))
//          }
//        }
      } ~ path("send") {
        parameters('recepient, 'fee.as[Long], 'change.as[Long], 'amount.as[Long]) { (recepient, fee, change, amount) =>
          val (prKey, pubKey) = PrivateKey25519.generateKeys("1".getBytes)
          val transaction: EncryTransaction = TransactionFactory.defaultPaymentTransactionScratch(
            prKey, fee, System.currentTimeMillis, ???, recepient.asInstanceOf[Address], amount, None)
          import encry.modifiers.mempool.EncryTransaction._
          import io.circe.syntax._
          transaction.asJson.trace

          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, page(jumbModalButton, modal).render))
        }
      } ~ path("test" ) {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, example.render))
      }

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

      println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}