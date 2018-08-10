package modules

import com.google.inject.{AbstractModule, Provides}
import com.typesafe.config.ConfigFactory
import play.api.Configuration
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import net.codingwell.scalaguice.ScalaModule
import settings.WalletAppSettings
import settings.WalletAppSettings._

class MainModule extends AbstractModule with ScalaModule {

  @Provides
  def provideWalletAppSettings(configuration: Configuration): WalletAppSettings = {

    val settings: WalletAppSettings = ConfigFactory.load("local.conf")
      .withFallback(configuration.underlying).as[WalletAppSettings]("encry")

    if (settings.knownPeers.nonEmpty) settings else throw new RuntimeException("Invalid configuration 'knownPeers' should not be empty")

  }

}
