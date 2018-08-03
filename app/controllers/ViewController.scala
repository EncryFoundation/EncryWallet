package controllers

import akka.http.scaladsl.model.StatusCodes
import javax.inject.{Inject, Singleton}
import models._
import play.api.data.Forms._
import play.api.data.{Form, Mapping}
import play.api.i18n.I18nSupport
import play.api.mvc._
import scorex.crypto.encode.{Base16, Base58}
import services._
import storage.LSMStorage
import scala.concurrent.{ExecutionContext, ExecutionException, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

@Singleton
class ViewController @Inject()(implicit ec: ExecutionContext, ts: TransactionService, ws: WalletService, lsmStorage: LSMStorage, es: ExplorerService, cc: ControllerComponents)
  extends AbstractController(cc) with I18nSupport {

  var wallet: Option[Wallet] = None

  val base16text: Mapping[String] = text.verifying(Base16.decode(_).isSuccess)

  val base58text: Mapping[String] = text.verifying(Base58.decode(_).isSuccess)

  val paymentTransactionRequestForm: Form[PaymentTransactionRequest] = Form(
    mapping(
      "fee" -> longNumber,
      "amount" -> longNumber,
      "recipient" -> base58text
    )(PaymentTransactionRequest.apply)(PaymentTransactionRequest.unapply))

  val scriptedTransactionRequestForm: Form[ScriptedTransactionRequest] = Form(
    mapping(
      "fee" -> longNumber,
      "amount" -> longNumber,
      "source" -> text
    )(ScriptedTransactionRequest.apply)(ScriptedTransactionRequest.unapply))

  def showPaymentTransactionForm(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    wallet match {
      case Some(w) => Ok(views.html.payment(Base16.encode(w.pubKey), paymentTransactionRequestForm))
      case None => Redirect(routes.ViewController.message("You should set up a wallet before making transaction"))
    }
  }

  def sendPaymentTransactionFromForm(walletId: String): Action[AnyContent] = Action.async {
    implicit request =>
      paymentTransactionRequestForm.bindFromRequest.fold(
        errors => Future.successful(Redirect(routes.ViewController.message("Wrong transaction parameters\n" + errors.errors.mkString("\n") + errors))),
        ptr => ts.sendPaymentTransaction(walletId, ptr).map {
          _.status match {
            case StatusCodes.OK => Redirect(routes.ViewController.message("The transaction has been successfully sent to blockchain"))
            case errorCode => Redirect(routes.ViewController.message("Sending trasaction to the node has failed with code: " + errorCode))
          }
        }.recover {
          case e: IllegalArgumentException => Redirect(routes.ViewController.message(e.getMessage))
          case e: ExecutionException => Redirect(routes.ViewController.message(e.getCause.getMessage + "\n" + e.getCause.getStackTrace.mkString("\n")))
          case NonFatal(e) => Redirect(routes.ViewController.message(e.getMessage + "\n" + e.getStackTrace.mkString("\n")))
        }
      )
  }

  def showScriptedTransactionForm(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    wallet match {
      case Some(w) => Ok(views.html.scripted(Base16.encode(w.pubKey), scriptedTransactionRequestForm))
      case None => Redirect(routes.ViewController.message("You should set up a wallet before making transaction"))
    }
  }

  def sendScriptedTransactionFromForm(walletId: String): Action[AnyContent] = Action.async {
    implicit request =>
      scriptedTransactionRequestForm.bindFromRequest.fold(
        errors => Future.successful(Redirect(routes.ViewController.message("Wrong transaction parameters\n" + errors.errors.mkString("\n") + errors))),
        str => ts.sendScriptedTransaction(walletId, str).map {
          _.status match {
            case StatusCodes.OK => Redirect(routes.ViewController.message("The transaction has been successfully sent to blockchain"))
            case errorCode => Redirect(routes.ViewController.message("Sending trasaction to the node has failed with code: " + errorCode))
          }
        }.recover {
          case e: IllegalArgumentException => Redirect(routes.ViewController.message(e.getMessage))
          case e: ExecutionException => Redirect(routes.ViewController.message(e.getCause.getMessage + "\n" + e.getCause.getStackTrace.mkString("\n")))
          case NonFatal(e) => Redirect(routes.ViewController.message(e.getMessage + "\n" + e.getStackTrace.mkString("\n")))
        }
      )
  }

  def message(msg: String): Action[AnyContent] = Action {
    Ok(views.html.message(msg))
  }

  val settingsForm: Form[ViewController.SettingsData] = Form(
    mapping(
      "secretKey" -> base58text
    )(ViewController.SettingsData.apply)(ViewController.SettingsData.unapply)
  )

  def setWallet(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    settingsForm.bindFromRequest.fold(
      _ => Redirect(routes.ViewController.message("Invalid key")),
      data => {
        ws.restoreFromSecret(data.secretKey) match {
          case Success(w) =>
            wallet = Some(w)
            Redirect(routes.ViewController.message("Wallet has been successfully set"))
          case Failure(_) => Redirect(routes.ViewController.message("Can not restore wallet from this key"))
        }
      }
    )
  }

  def showSettingsForm(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.settings(settingsForm.fill(ViewController.SettingsData(wallet.flatMap(w => Try(lsmStorage.getWalletSecret(w)).toOption).map(_.privKeyBytes).map(Base58.encode).getOrElse("")))))
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

  case class SettingsData(secretKey: String)

}
