package controllers

import akka.http.scaladsl.model.StatusCodes
import javax.inject.Inject
import play.api.libs.circe.Circe
import play.api.mvc._
import services.ExplorerService
import models._
import org.encryfoundation.prismlang.compiler.PCompiler.compile
import storage.LSMStorage

import scala.concurrent.{ExecutionContext, Future}

class TransactionController @Inject()(implicit ec: ExecutionContext, es: ExplorerService, cc: ControllerComponents) extends AbstractController(cc) with Circe {

  def sendPaymentTransaction(walletId: String): Action[PaymentTransactionRequest] = Action(circe.json[PaymentTransactionRequest]).async {
    implicit request: Request[PaymentTransactionRequest] =>
      val r = request.body
      Wallet.fromId(walletId) match {
        case Some(w) => es.requestUtxos(w.account.address).map{ boxes =>
          Transaction.defaultPaymentTransactionScratch(
            LSMStorage.getWalletSecret(w),
            r.fee,
            System.currentTimeMillis,
            boxes,
            r.recipient,
            r.amount,
            None
          )
        } flatMap {
          es.commitTransaction
        } map { _.status match {
          case StatusCodes.OK => Ok("")
          case _              => InternalServerError("")
        } }
        case None => Future.successful(BadRequest("Wallet ID is invalid"))
      }
  }

  def sendScriptedTransaction(walletId: String): Action[ScriptedTransactionRequest] = Action(circe.json[ScriptedTransactionRequest]).async {
    implicit request: Request[ScriptedTransactionRequest] =>
      val r = request.body
      Wallet.fromId(walletId) match {
        case Some(w) => es.requestUtxos(w.account.address) flatMap { boxes =>
          compile(r.source).map { contract =>
            Transaction.scriptedAssetTransactionScratch(
              LSMStorage.getWalletSecret(w),
              r.fee,
              System.currentTimeMillis,
              boxes,
              contract,
              r.amount,
              None
            )
          } fold(Future.failed, Future.successful)
        } flatMap {
          es.commitTransaction
        } map { _.status match {
          case StatusCodes.OK => Ok("")
          case _              => InternalServerError("")
        } }
        case None => Future.successful(BadRequest("Wallet ID is invalid"))
      }
  }

}
