package org.encryfoundation.wallet.settings

import java.net.InetSocketAddress

import scala.concurrent.duration.FiniteDuration

case class RESTApiSettings(bindAddress: InetSocketAddress,
                           timeout: FiniteDuration)
