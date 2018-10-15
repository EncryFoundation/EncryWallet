package services

import javax.inject.Inject

import scala.concurrent.{ExecutionContext, Future}
import akka.http.scaladsl.model.HttpResponse
import org.encryfoundation.prismlang.compiler.{CompiledContract, PCompiler}
import org.encryfoundation.common.transaction.{Proof, PubKeyLockedContract}
import org.encryfoundation.common.Algos
import storage.LSMStorage
import models._
import org.encryfoundation.common.crypto.PrivateKey25519

import scala.util.Random

class TransactionService @Inject()(implicit ec: ExecutionContext, lsmStrorage: LSMStorage, es: ExplorerService) {

  def sendPaymentTransaction(walletId: String, ptr: PaymentTransactionRequest): Future[HttpResponse] = {
    sendPaymentTransactionWithInputIds(walletId, ptr, Seq.empty)
  }

  def sendScriptedTransaction(walletId: String, str: ScriptedTransactionRequest): Future[HttpResponse] = {
    sendScriptedTransactionWithInputsIds(walletId, str, Seq.empty)
  }

  def sendDataTransaction(walletId: String, dtr: DataTransactionRequest): Future[HttpResponse] = {
    sendDataTransactionWithInputsIds(walletId, dtr)
  }

  def sendDataTransactionWithInputsIds(walletId: String,
                                       dtr: DataTransactionRequest): Future[HttpResponse] = {
    Wallet(walletId) match {
      case Some(w) =>

        val outputsF: Future[Seq[(Output, Option[(CompiledContract, Seq[Proof])])]] =
           es.requestUtxos(w.address.address).map(x => x.map((_, None)))

        outputsF.map { outputs =>

          if (outputs.isEmpty) throw new RuntimeException("Transaction impossible: no inputs available")

          if (dtr.data.getBytes.length > 1000) throw new RuntimeException("Transaction impossible: data size > 1000 bytes")

          val secret: PrivateKey25519 = lsmStrorage.getWalletSecret(w)

          Transaction.dataTransactionScratch(
            secret,
            dtr.fee,
            System.currentTimeMillis,
            Random.shuffle(outputs),
            PubKeyLockedContract(secret.publicImage.pubKeyBytes).contract,
            dtr.amount,
            dtr.data.getBytes
          )
        }.flatMap {
          es.commitTransaction
        }
      case None => Future.failed(new IllegalArgumentException("Wallet ID is not a valid Base16 encoded string"))
    }
  }

  def sendPaymentTransactionWithInputIds(walletId: String,
                                         ptr: PaymentTransactionRequest,
                                         inputs: Seq[ParsedInput]): Future[HttpResponse] = {
    Wallet(walletId) match {
      case Some(w) =>

        val outputsF: Future[Seq[(Output, Option[(CompiledContract, Seq[Proof])])]] =
          if (inputs.isEmpty) es.requestUtxos(w.address.address).map(x => x.map((_, None)))
          else Future.sequence(inputs.map(x => es.requestOutput(Algos.encode(x.key)).map(_ -> x.contract)))

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

  def sendScriptedTransactionWithInputsIds(walletId: String,
                                           str: ScriptedTransactionRequest,
                                           inputs: Seq[ParsedInput]): Future[HttpResponse] = {
    Wallet(walletId) match {
      case Some(w) =>
        Future.fromTry(PCompiler.compile(str.source.replaceAll("\r\n", "\n"))).flatMap { contract =>

          val outputsF: Future[Seq[(Output, Option[(CompiledContract, Seq[Proof])])]] =
            if (inputs.isEmpty) es.requestUtxos(w.address.address).map(x => x.map((_, None)))
            else Future.sequence(inputs.map(x => es.requestOutput(Algos.encode(x.key)).map(_ -> x.contract)))

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