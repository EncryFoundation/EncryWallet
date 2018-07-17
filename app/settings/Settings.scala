package settings

import java.io.File
import java.net.InetSocketAddress

import com.typesafe.config.{Config, ConfigFactory}
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ValueReader

import scala.concurrent.duration.FiniteDuration

case class RESTApiSettings(bindAddress: InetSocketAddress,
                           timeout: FiniteDuration)


case class WalletAppSettings(knownPeers: List[InetSocketAddress],
                             explorerAddress: InetSocketAddress,
                             restApi: RESTApiSettings)

object WalletAppSettings  {

  implicit val fileReader: ValueReader[File] = (cfg, path) => new File(cfg.getString(path))
  implicit val byteValueReader: ValueReader[Byte] = (cfg, path) => cfg.getInt(path).toByte
  implicit val inetSocketAddressReader: ValueReader[InetSocketAddress] = { (config: Config, path: String) =>
    val split = config.getString(path).split(":")
    new InetSocketAddress(split(0), split(1).toInt)
  }

}