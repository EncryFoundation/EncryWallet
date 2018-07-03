package org.encryfoundation.wallet.actions

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.util.ByteString
import io.circe.parser.decode
import io.circe.syntax._
import org.encryfoundation.wallet.WalletApp
import org.encryfoundation.wallet.deprecated.Implicits._
import org.encryfoundation.wallet.settings.WalletAppSettings
import org.encryfoundation.wallet.transaction.EncryTransaction
import org.encryfoundation.wallet.transaction.box.AssetBox

import scala.concurrent.Future

object NetworkActions {

  val settings: WalletAppSettings = WalletApp.settings

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
      uri = Uri(s"${settings.explorerAddress}/transactions//send"),
      entity = HttpEntity(ContentTypes.`application/json`, tx.asJson.toString)
    ))
}
