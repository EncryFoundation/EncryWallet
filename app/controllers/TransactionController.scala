package controllers

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import akka.http.scaladsl.model.StatusCodes
import play.api.mvc._
import play.api.libs.circe.Circe
import models._
import services.TransactionService

class TransactionController @Inject()(implicit ec: ExecutionContext, ts: TransactionService, cc: ControllerComponents)
  extends AbstractController(cc) with Circe {

  def sendPaymentTransaction(walletId: String): Action[PaymentTransactionRequest] = Action(circe.json[PaymentTransactionRequest]).async {
    implicit request: Request[PaymentTransactionRequest] =>
      ts.sendPaymentTransaction(walletId, request.body).map {
        _.status match {
          case StatusCodes.OK => Ok
          case _ => InternalServerError("Explorer node is down")
        }
      }.recover {
        case e: IllegalArgumentException => BadRequest(e.getMessage)
        case _ => InternalServerError
      }
  }

  def sendScriptedTransaction(walletId: String): Action[ScriptedTransactionRequest] = Action(circe.json[ScriptedTransactionRequest]).async {
    implicit request: Request[ScriptedTransactionRequest] =>
      ts.sendScriptedTransaction(walletId, request.body).map {
        _.status match {
          case StatusCodes.OK => Ok
          case _ => InternalServerError("Explorer node is down")
        }
      }.recover {
        case e: IllegalArgumentException => BadRequest(e.getMessage)
        case _ => InternalServerError
      }
  }

}
