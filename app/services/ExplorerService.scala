package services

import javax.inject.Inject
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.Materializer
import akka.util.ByteString
import play.api.mvc.ControllerComponents
import io.circe.parser.decode
import io.circe.syntax._
import settings.WalletAppSettings
import models.box.AssetBox
import models.EncryTransaction

import scala.concurrent.Future

class ExplorerService @Inject()(implicit val system: ActorSystem, implicit val materializer: Materializer, settings: WalletAppSettings, cc: ControllerComponents) {

  import system.dispatcher

  def requestUtxos(address: String): Future[IndexedSeq[AssetBox]] =
    Http().singleRequest(HttpRequest(
      method = HttpMethods.GET,
      uri = s"${settings.explorerAddress}/transactions/$address/outputs/unspent"
    )).flatMap(_.entity.dataBytes.runFold(ByteString.empty)(_ ++ _))
      .map(_.utf8String)
      .map(decode[Seq[AssetBox]])
      .map(_.map(_.toIndexedSeq))
      .flatMap(_.fold(Future.failed, Future.successful))

  def commitTransaction(tx: EncryTransaction): Future[HttpResponse] =
    Http().singleRequest(HttpRequest(
      method = HttpMethods.POST,
      uri = Uri(s"${settings.explorerAddress}/transactions/send"),
      entity = HttpEntity(ContentTypes.`application/json`, tx.asJson.toString)
    ))

}
