package services

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import akka.http.scaladsl.model.HttpResponse
import org.encryfoundation.prismlang.compiler.PCompiler
import models._
import storage.LSMStorage

class TransactionService @Inject()(implicit ec: ExecutionContext, lsmStrorage: LSMStorage, es: ExplorerService) {

  def sendPaymentTransaction(walletId: String, ptr: PaymentTransactionRequest): Future[HttpResponse] = {
    sendPaymentTransactionWithInputIds(walletId, ptr, Seq.empty)
  }

  def sendScriptedTransaction(walletId: String, str: ScriptedTransactionRequest): Future[HttpResponse] = {
    sendScriptedTransactionWithInputsIds(walletId, str, Seq.empty)
  }

  def sendPaymentTransactionWithInputIds(walletId: String, ptr: PaymentTransactionRequest, inputIds: Seq[(String, String)]): Future[HttpResponse] = {
    Wallet.fromId(walletId) match {
      case Some(w) =>

        val outputsF: Future[Seq[(Output, String)]] =
          if (inputIds.isEmpty) es.requestUtxos(w.address.address).map(x => x.map((_, "")))
          else Future.sequence(inputIds.map(x => es.requestOutput(x._1).map((_, x._2))))

        outputsF.map { outputs =>

          if (outputs.isEmpty) throw new RuntimeException("Transaction impossible: no inputs available")

          Transaction.defaultPaymentTransactionScratch(
            lsmStrorage.getWalletSecret(w),
            ptr.fee,
            System.currentTimeMillis,
            outputs,
            ptr.recipient,
            ptr.amount,
            None
          )
        }.flatMap {
          es.commitTransaction
        }
      case None => Future.failed(new IllegalArgumentException("Wallet ID is not a valid Base16 encoded string"))
    }
  }

  def sendScriptedTransactionWithInputsIds(walletId: String, str: ScriptedTransactionRequest, inputIds: Seq[(String, String)]): Future[HttpResponse] = {
    Wallet.fromId(walletId) match {
      case Some(w) =>
        Future.fromTry(PCompiler.compile(str.source)).flatMap { contract =>

          val outputsF: Future[Seq[(Output, String)]] =
            if (inputIds.isEmpty) es.requestUtxos(w.address.address).map(x => x.map((_, "")))
            else Future.sequence(inputIds.map(x => es.requestOutput(x._1).map((_, x._2))))

          outputsF.map { outputs =>

            if (outputs.isEmpty) throw new RuntimeException("Transaction impossible: no inputs available")

            Transaction.scriptedAssetTransactionScratch(
              lsmStrorage.getWalletSecret(w),
              str.fee,
              System.currentTimeMillis,
              outputs,
              contract,
              str.amount,
              None
            )
          }
        }.flatMap {
          es.commitTransaction
        }
      case None => Future.failed(new IllegalArgumentException("Wallet ID is not a valid Base16 encoded string"))
    }
  }


}
