package controllers

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, ExecutionException, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import play.api.data.Forms._
import play.api.data.{Form, Mapping}
import play.api.i18n.I18nSupport
import play.api.mvc._
import scorex.crypto.encode.{Base16, Base58}
import models._
import services._
import storage.LSMStorage

@Singleton
class ViewController @Inject()(implicit ec: ExecutionContext, ts: TransactionService, ws: WalletService, lsmStorage: LSMStorage, es: ExplorerService, cc: ControllerComponents)
  extends AbstractController(cc) with I18nSupport {

  private var wallet: Option[Wallet] = None

  private def handleSendTransactionResponse(f: Future[HttpResponse]): Future[Result] = {
    f.map {
      _.status match {
        case StatusCodes.OK => Redirect(routes.ViewController.message("The transaction has been successfully sent to blockchain"))
        case errorCode => Redirect(routes.ViewController.message("Sending trasaction to the node has failed with code: " + errorCode))
      }
    }.recover {
      case e: IllegalArgumentException => Redirect(routes.ViewController.message(e.getMessage))
      case e: ExecutionException => Ok(views.html.message(e.getCause.getMessage + "\n" + e.getCause.getStackTrace.mkString("\n")))
      case NonFatal(e) => Ok(views.html.message(e.getMessage + "\n" + e.getStackTrace.mkString("\n")))
    }
  }

  def showPaymentTransactionForm(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    wallet match {
      case Some(w) => Ok(views.html.payment(Base16.encode(w.pubKey), ViewController.paymentTransactionRequestForm))
      case None => Redirect(routes.ViewController.message("You should set up a wallet before making transaction"))
    }
  }

  def sendPaymentTransactionFromForm(walletId: String): Action[AnyContent] = Action.async {
    implicit request =>
      ViewController.paymentTransactionRequestForm.bindFromRequest.fold(
        errors => Future.successful(Redirect(routes.ViewController.message("Wrong transaction parameters\n" + errors.errors.mkString("\n") + errors))),
        ptrd => {
          val inputIds: Seq[(String, String)] = ViewController.parseInputs(ptrd.inputsIds)
          if (inputIds.forall(x => Base16.decode(x._1).isSuccess))
            handleSendTransactionResponse(ts.sendPaymentTransactionWithInputIds(walletId, ptrd.paymentTransactionRequest, inputIds))
          else Future.successful(Redirect(routes.ViewController.message("Inputs IDs are not valid Base16 strings")))
        }
      )
  }

  def showScriptedTransactionForm(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    wallet match {
      case Some(w) => Ok(views.html.scripted(Base16.encode(w.pubKey), ViewController.scriptedTransactionRequestForm))
      case None => Redirect(routes.ViewController.message("You should set up a wallet before making transaction"))
    }
  }

  def sendScriptedTransactionFromForm(walletId: String): Action[AnyContent] = Action.async {
    implicit request =>
      ViewController.scriptedTransactionRequestForm.bindFromRequest.fold(
        errors => Future.successful(Redirect(routes.ViewController.message("Wrong transaction parameters\n" + errors.errors.mkString("\n") + errors))),
        strd => {
          val inputIds: Seq[(String, String)] = ViewController.parseInputs(strd.inputsIds)
          if (inputIds.forall(x => Base16.decode(x._1).isSuccess))
            handleSendTransactionResponse(ts.sendScriptedTransactionWithInputsIds(walletId, strd.scriptedTransactionRequest, inputIds))
          else Future.successful(Redirect(routes.ViewController.message("Inputs IDs are not valid Base16 strings")))
        }
      )
  }

  def message(msg: String): Action[AnyContent] = Action {
    Ok(views.html.message(msg))
  }

  def setWallet(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    ViewController.settingsForm.bindFromRequest.fold(
      _ => Redirect(routes.ViewController.message("Invalid key")),
      data => {
        Base16.decode(data.secretKey).map(ws.restoreFromSecret) match {
          case Success(w) =>
            wallet = Some(w)
            Redirect(routes.ViewController.message("Wallet has been successfully set"))
          case Failure(_) => Redirect(routes.ViewController.message("Can not restore wallet from this key"))
        }
      }
    )
  }

  def showSettingsForm(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    val privateKey: String = wallet.flatMap(w => Try(lsmStorage.getWalletSecret(w)).toOption).map(_.privKeyBytes).map(Base16.encode).getOrElse("")
    Ok(views.html.settings(ViewController.settingsForm.fill(ViewController.SettingsData(privateKey))))
  }

  def showWalletInfo(): Action[AnyContent] = Action.async { implicit request =>
    wallet match {
      case Some(w) =>
        val balanceF: Future[Option[Long]] = ws.loadWalletInfo(w).map(_.balance).map(Some(_)).recover { case NonFatal(_) => None }
        val hash: String = PubKeyLockedContract(w.pubKey).contractHashHex
        val outputsF: Future[Seq[Output]] = es.requestUtxos(w.address.address).recover { case NonFatal(_) => Seq.empty }
        for {
          balance <- balanceF
          outputs <- outputsF
        } yield Ok(views.html.wallet(w, balance, hash, outputs))
      case None => Future.successful(Redirect(routes.ViewController.message("You should set up a wallet before making transaction")))
    }
  }
}

object ViewController {

  val base16text: Mapping[String] = text.verifying(Base16.decode(_).isSuccess)

  val base58text: Mapping[String] = text.verifying(Base58.decode(_).isSuccess)

  val paymentTransactionRequestForm: Form[ViewController.PaymentTransactionRequestData] = Form(
    mapping(
      "paymentTransactionRequest" -> mapping(
        "fee" -> longNumber,
        "amount" -> longNumber,
        "recipient" -> base58text)
      (PaymentTransactionRequest.apply)(PaymentTransactionRequest.unapply),
      "inputsIds" -> text
    )(ViewController.PaymentTransactionRequestData.apply)(ViewController.PaymentTransactionRequestData.unapply))

  val scriptedTransactionRequestForm: Form[ViewController.ScriptedTransactionRequestData] = Form(
    mapping(
      "scriptedTransactionRequest" -> mapping(
        "fee" -> longNumber,
        "amount" -> longNumber,
        "source" -> text
      )(ScriptedTransactionRequest.apply)(ScriptedTransactionRequest.unapply),
      "inputsIds" -> text
    )(ViewController.ScriptedTransactionRequestData.apply)(ViewController.ScriptedTransactionRequestData.unapply))

  val settingsForm: Form[ViewController.SettingsData] = Form(
    mapping(
      "secretKey" -> base16text
    )(ViewController.SettingsData.apply)(ViewController.SettingsData.unapply)
  )

  case class PaymentTransactionRequestData(paymentTransactionRequest: PaymentTransactionRequest, inputsIds: String)

  case class ScriptedTransactionRequestData(scriptedTransactionRequest: ScriptedTransactionRequest, inputsIds: String)

  case class SettingsData(secretKey: String)

  def parseInputs(str: String): Seq[(String, String)] = {
    str
      .split("------")
      .filter(_.nonEmpty)
      .map(_.stripLineEnd.trim)
      .map(_.split(">>>>>>").filter(_.nonEmpty).map(_.stripLineEnd).toList)
      .map {
        case x :: Nil => (x, "")
        case x :: y :: Nil => (x, y)
        case _ => ("", "")
      }
      .filter(_._1.nonEmpty)
      .toSeq
  }

}
