package services

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Host
import akka.stream.Materializer
import akka.util.ByteString
import io.circe.parser.decode
import io.circe.syntax._
import models.Output
import models.EncryTransaction
import settings.WalletAppSettings

class ExplorerService @Inject()(implicit val system: ActorSystem, implicit val materializer: Materializer,
                                implicit val ec: ExecutionContext, settings: WalletAppSettings) {

  def requestUtxos(address: String): Future[Seq[Output]] =
    Http().singleRequest(HttpRequest(
      method = HttpMethods.GET,
      uri = s"/api/transactions/$address/outputs/unspent"
    ).withEffectiveUri(securedConnection = false, Host(settings.explorerAddress)))
      .flatMap(_.entity.dataBytes.runFold(ByteString.empty)(_ ++ _))
      .map(_.utf8String)
      .map(decode[Seq[Output]])
      .flatMap(_.fold(Future.failed, Future.successful))

  def commitTransaction(tx: EncryTransaction): Future[HttpResponse] =
    Http().singleRequest(HttpRequest(
      method = HttpMethods.POST,
      uri = "/transactions/send",
      entity = HttpEntity(ContentTypes.`application/json`, tx.asJson.toString)
    ).withEffectiveUri(securedConnection = false, Host(settings.knownPeers.head)))

  def requestOutput(id: String): Future[Output] =
    Http().singleRequest(HttpRequest(
      method = HttpMethods.GET,
      uri = s"/api/transactions/output/$id"
    ).withEffectiveUri(securedConnection = false, Host(settings.explorerAddress)))
      .flatMap(_.entity.dataBytes.runFold(ByteString.empty)(_ ++ _))
      .map(_.utf8String)
      .map(decode[Option[Output]])
      .flatMap(_.fold(Future.failed, Future.successful))
      .flatMap(_.map(Future.successful).getOrElse(Future.failed(new NoSuchElementException)))



}
