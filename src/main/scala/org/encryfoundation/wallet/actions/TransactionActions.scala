package org.encryfoundation.wallet.actions

import akka.http.scaladsl.model._
import org.encryfoundation.prismlang.compiler.PCompiler.compile
import org.encryfoundation.wallet.Wallet
import org.encryfoundation.wallet.deprecated.Implicits._
import org.encryfoundation.wallet.transaction.Transaction
import org.encryfoundation.wallet.utils.ExtUtils._

import scala.concurrent.Future

object TransactionActions {

  import NetworkActions._

  def sendPaymentTransaction(fee: Long, amount: Long, recipient: String)(wallet: Wallet): Future[HttpResponse] =
    requestUtxos(wallet.account.address).flatMap { boxes =>
      Transaction.defaultPaymentTransactionScratch(
        wallet.getSecret,
        fee,
        System.currentTimeMillis,
        boxes,
        recipient,
        amount,
        None
      ).rapply(commitTransaction)
    }

  def sendScriptedTransaction(fee: Long, amount: Long, script: String)(wallet: Wallet): Future[HttpResponse] =
    requestUtxos(wallet.account.address).flatMap { boxes =>
      compile(script).map { contract =>
        Transaction.scriptedAssetTransactionScratch(
          wallet.getSecret,
          fee,
          System.currentTimeMillis,
          boxes,
          contract,
          amount,
          None
        )
      }.map(commitTransaction).fold(Future.failed, x => x)
    }
}
