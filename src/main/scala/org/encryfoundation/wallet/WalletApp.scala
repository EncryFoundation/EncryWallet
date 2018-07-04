package org.encryfoundation.wallet

import java.io.File
import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._
import io.iohk.iodb.LSMStore
import org.encryfoundation.wallet.http.api.{ApiRoute, WalletApiRoute}
import org.encryfoundation.wallet.settings.WalletAppSettings

import scala.concurrent._

object WalletApp extends App {

  implicit val system: ActorSystem = ActorSystem("wallet")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  lazy val settings: WalletAppSettings = WalletAppSettings.read

  val bindAddress: InetSocketAddress = settings.restApi.bindAddress

  val routes: Seq[ApiRoute] = Seq(
    WalletApiRoute()
  )

  Http().bindAndHandle(routes.map(_.route).reduce(_ ~ _), bindAddress.getAddress.getHostAddress, bindAddress.getPort)

  val dir: File = new File("keys")
  dir.mkdirs()

  lazy val store: LSMStore = new LSMStore(dir, keepVersions = 0)
}
