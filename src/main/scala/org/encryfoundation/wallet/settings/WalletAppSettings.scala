package org.encryfoundation.wallet.settings

import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

case class WalletAppSettings(knownPeers: List[String],
                             explorerAddress: String,
                             restApi: RESTApiSettings)

object WalletAppSettings extends SettingsReader {

  val read: WalletAppSettings = ConfigFactory.load("local.conf")
    .withFallback(ConfigFactory.load).as[WalletAppSettings]("encry")
}
