package controllers

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, ExecutionException, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import cats.implicits._
import fastparse.all._
import fastparse.core.{ParseError, Parsed, Parser}
import play.api.data.Forms._
import play.api.data.{Form, Mapping}
import play.api.i18n.I18nSupport
import play.api.mvc._
import scorex.crypto.encode.Base58
import org.encryfoundation.common.Algos
import org.encryfoundation.common.transaction.{Proof, PubKeyLockedContract}
import org.encryfoundation.prismlang.core.wrapped.BoxedValue
import org.encryfoundation.prismlang.core.wrapped.BoxedValue._
import org.encryfoundation.prismlang.parser.{Expressions, Lexer}
import org.encryfoundation.prismlang.compiler.PCompiler
import storage.LSMStorage
import services._
import models._
import org.encryfoundation.common.utils.TaggedTypes.ADKey
import utils.Mnemonic

@Singleton
class ViewController @Inject()(implicit ec: ExecutionContext,
                               ts: TransactionService,
                               ws: WalletService,
                               lsmStorage: LSMStorage,
                               es: ExplorerService,
                               cc: ControllerComponents) extends AbstractController(cc) with I18nSupport {

  private def wallet(implicit request: Request[AnyContent]): Option[Wallet] =
    request.cookies.get(ViewController.pubKeyCookieName).flatMap(x => Wallet(x.value))

  private def handleSendTransactionResponse(f: Future[HttpResponse]): Future[Result] = {
    f.map {
      _.status match {
        case StatusCodes.OK =>
          Redirect(routes.ViewController.message("The transaction has been successfully sent to blockchain"))
        case errorCode =>
          Redirect(routes.ViewController.message("Sending trasaction to the node has failed with code: " + errorCode))
      }
    }.recover {
      case e: IllegalArgumentException => Redirect(routes.ViewController.message(e.getMessage))
      case e: ExecutionException =>
        Ok(views.html.message(e.getCause.getMessage + "\n" + e.getCause.getStackTrace.mkString("\n")))
      case NonFatal(e) => Ok(views.html.message(e.getMessage))
    }
  }

  def showPaymentTransactionForm(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    wallet match {
      case Some(w) => Ok(views.html.payment(Algos.encode(w.pubKey), ViewController.paymentTransactionRequestForm))
      case None => Redirect(routes.ViewController.message("You should set up a wallet before making transaction"))
    }
  }

  def sendPaymentTransactionFromForm(walletId: String): Action[AnyContent] = Action.async {
    implicit request: Request[AnyContent] =>
      ViewController.paymentTransactionRequestForm.bindFromRequest.fold(
        errors =>
          Future.successful(
            Redirect(routes.ViewController.message(
              "Wrong transaction parameters\n" + errors.errors.mkString("\n") + errors))
          ),
        ptrd => {
          val inputsE: Either[Throwable, Seq[ParsedInput]] = ViewController.parseInputs(ptrd.inputsIds)
          inputsE match {
            case Right(v) =>
              handleSendTransactionResponse(
                ts.sendPaymentTransactionWithInputIds(walletId, ptrd.paymentTransactionRequest, v)
              )
            case Left(f) => Future.successful(Redirect(routes.ViewController.message(f.getMessage)))
          }
        }
      )
  }

  def showDataTransactionForm(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    wallet match {
      case Some(w) => Ok(views.html.data(Algos.encode(w.pubKey), ViewController.dataTransactionRequestForm))
      case None => Redirect(routes.ViewController.message("You should set up a wallet before making transaction"))
    }
  }

  def sendDataTransactionFromForm(walletId: String): Action[AnyContent] = Action.async {
    implicit request: Request[AnyContent] =>
      ViewController.dataTransactionRequestForm.bindFromRequest.fold(
        errors =>
          Future.successful(
            Redirect(routes.ViewController.message(
              "Wrong transaction parameters\n" + errors.errors.mkString("\n") + errors))
          ),
        ptrd => {
          handleSendTransactionResponse(
            ts.sendDataTransactionWithInputsIds(walletId, ptrd.dataTransactionRequest)
          )
        })
  }

  def showScriptedTransactionForm(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    wallet match {
      case Some(w) => Ok(views.html.scripted(Algos.encode(w.pubKey), ViewController.scriptedTransactionRequestForm))
      case None => Redirect(routes.ViewController.message("You should set up a wallet before making transaction"))
    }
  }

  def sendScriptedTransactionFromForm(walletId: String): Action[AnyContent] = Action.async {
    implicit request: Request[AnyContent] =>
      ViewController.scriptedTransactionRequestForm.bindFromRequest.fold(
        errors => Future.successful(
          Redirect(routes.ViewController.message(
            "Wrong transaction parameters\n" + errors.errors.mkString("\n") + errors))
        ),
        strd => {
          val inputsE: Either[Throwable, Seq[ParsedInput]] = ViewController.parseInputs(strd.inputsIds)
          inputsE match {
            case Right(v) =>
              handleSendTransactionResponse(
                ts.sendScriptedTransactionWithInputsIds(walletId, strd.scriptedTransactionRequest, v)
              )
            case Left(f) => Future.successful(Redirect(routes.ViewController.message(f.getMessage)))
          }
        }
      )
  }

  def showAssetIssuingTransactionForm(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    wallet match {
      case Some(w) => Ok(views.html.issuing(Algos.encode(w.pubKey), ViewController.assetIssuingTransactionRequestForm))
      case None => Redirect(routes.ViewController.message("You should set up a wallet before making transaction"))
    }
  }

  def sendAssetIssuingTransactionFromForm(walletId: String): Action[AnyContent] = Action.async {
    implicit request: Request[AnyContent] =>
      ViewController.scriptedTransactionRequestForm.bindFromRequest.fold(
        errors => Future.successful(
          Redirect(routes.ViewController.message(
            "Wrong transaction parameters\n" + errors.errors.mkString("\n") + errors))
        ),
        strd => {
          val inputsE: Either[Throwable, Seq[ParsedInput]] = ViewController.parseInputs(strd.inputsIds)
          inputsE match {
            case Right(v) =>
              handleSendTransactionResponse(
                ts.sendScriptedTransactionWithInputsIds(walletId, strd.scriptedTransactionRequest, v)
              )
            case Left(f) => Future.successful(Redirect(routes.ViewController.message(f.getMessage)))
          }
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
        Algos.decode(data.secretKey).map(ws.restoreFromSecret) match {
          case Success(w) =>
            Redirect(routes.ViewController.message("Wallet has been successfully set"))
              .withCookies(Cookie(ViewController.pubKeyCookieName, Algos.encode(w.pubKey), Some(86400 * 5)))
          case Failure(_) => Redirect(routes.ViewController.message("Can not restore wallet from this key"))
        }
      }
    )
  }

  def setWalletWithMnemonic(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    ViewController.settingsMnemonicForm.bindFromRequest.fold(
      _ => Redirect(routes.ViewController.message("Mnemonic cod can not be empty")),
      data => {
        val w: Wallet = ws.createNewWallet(Mnemonic.seedFromMnemonic(data.mnemonicCode).some)
        Redirect(routes.ViewController.message("Wallet has been successfully set"))
          .withCookies(Cookie(ViewController.pubKeyCookieName, Algos.encode(w.pubKey), Some(86400 * 5)))
      }
    )
  }

  def createAndSetWalletWithOutSeed(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    val (w: Wallet, phrase: String) = ws.createNewWalletWithOutSeed
    Redirect(routes.ViewController.message(s"Wallet has been successfully created with mnemonic phrase: $phrase"))
      .withCookies(Cookie(ViewController.pubKeyCookieName, Algos.encode(w.pubKey), Some(86400 * 5)))
  }

  def showSettingsForm(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    val privateKey: String = wallet
      .flatMap(w => Try(lsmStorage.getWalletSecret(w)).toOption)
      .map(_.privKeyBytes)
      .map(Algos.encode)
      .getOrElse("")
    Ok(views.html.settings(
      ViewController.settingsForm.fill(ViewController.SettingsData(privateKey)),
      ViewController.settingsMnemonicForm.fill(ViewController.SettingsMnemonicData(""))
    ))
  }

  def showWalletInfo(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    wallet match {
      case Some(w) =>
        val balanceF: Future[Option[Long]] = es
          .requestBalance(w.address.address)
          .recover { case NonFatal(_) => None }
        val hash: String = PubKeyLockedContract(w.pubKey).contractHashHex
        val outputsF: Future[Seq[Output]] = es
          .requestUtxosWithLimit(w.address.address, 300)
          .map(_.sortWith(_.data > _.data))
          .recover { case NonFatal(_) => Seq.empty }
        for {
          balance <- balanceF
          outputs <- outputsF
        } yield Ok(views.html.wallet(w, balance, hash, outputs))
      case None => Future.successful(
        Redirect(routes.ViewController.message(
          "You should set up a wallet before making transaction"))
      )
    }
  }

}

object ViewController {

  val pubKeyCookieName: String = "pubKey"

  val base16text: Mapping[String] = text.verifying(Algos.decode(_).isSuccess)

  val base58text: Mapping[String] = text.verifying(Base58.decode(_).isSuccess)

  val dataTransactionRequestForm: Form[DataTransactionRequestData] = Form(
    mapping(
      "dataTransactionRequest" -> mapping(
        "fee" -> longNumber,
        "amount" -> longNumber,
        "data" -> text)
      (DataTransactionRequest.apply)(DataTransactionRequest.unapply)
    )(DataTransactionRequestData.apply)(DataTransactionRequestData.unapply))

  val paymentTransactionRequestForm: Form[PaymentTransactionRequestData] = Form(
    mapping(
      "paymentTransactionRequest" -> mapping(
        "fee" -> longNumber,
        "amount" -> longNumber,
        "recipient" -> base58text)
      (PaymentTransactionRequest.apply)(PaymentTransactionRequest.unapply),
      "inputsIds" -> text
    )(PaymentTransactionRequestData.apply)(PaymentTransactionRequestData.unapply))

  val scriptedTransactionRequestForm: Form[ScriptedTransactionRequestData] = Form(
    mapping(
      "scriptedTransactionRequest" -> mapping(
        "fee" -> longNumber,
        "amount" -> longNumber,
        "source" -> text
      )(ScriptedTransactionRequest.apply)(ScriptedTransactionRequest.unapply),
      "inputsIds" -> text
    )(ScriptedTransactionRequestData.apply)(ScriptedTransactionRequestData.unapply))

  val assetIssuingTransactionRequestForm: Form[AssetIssuingTransactionRequestData] = Form(
    mapping(
      "assetIssuingTransactionRequest" -> mapping(
        "fee" -> longNumber,
        "amount" -> longNumber,
        "source" -> text
      )(AssetIssuingTransactionRequest.apply)(AssetIssuingTransactionRequest.unapply),
      "inputsIds" -> text
    )(AssetIssuingTransactionRequestData.apply)(AssetIssuingTransactionRequestData.unapply))

  val settingsForm: Form[SettingsData] = Form(
    mapping(
      "secretKey" -> base16text
    )(SettingsData.apply)(SettingsData.unapply)
  )

  val settingsMnemonicForm: Form[SettingsMnemonicData] = Form(
    mapping(
      "mnemonicCode" -> nonEmptyText
    )(SettingsMnemonicData.apply)(SettingsMnemonicData.unapply)
  )

  case class PaymentTransactionRequestData(paymentTransactionRequest: PaymentTransactionRequest, inputsIds: String)

  case class ScriptedTransactionRequestData(scriptedTransactionRequest: ScriptedTransactionRequest, inputsIds: String)

  case class AssetIssuingTransactionRequestData(assetIssuingTransactionRequest: AssetIssuingTransactionRequest,
                                                inputsIds: String)

  case class DataTransactionRequestData(dataTransactionRequest: DataTransactionRequest)

  case class SettingsData(secretKey: String)

  case class SettingsMnemonicData(mnemonicCode: String)

  def parseInputs(str: String): Either[Throwable, Seq[ParsedInput]] = {
    str
      .split("------")
      .map(_.split(">>>>>>").filter(_.nonEmpty).map(_.stripLineEnd.trim).toList)
      .filter(_.nonEmpty)
      .map {
        case id :: Nil => Algos.decode(id).map(ADKey @@ _).map(x => ParsedInput(key = x))
        case id :: contractSource :: Nil =>
          for {
            i <- Algos.decode(id).map(ADKey @@ _)
            c <- PCompiler.compile(contractSource.replaceAll("\r\n", "\n"))
          } yield ParsedInput(i, Some(c -> Seq.empty))
        case id :: contractSource :: contractArgs :: Nil =>
          for {
            i <- Algos.decode(id).map(ADKey @@ _)
            c <- PCompiler.compile(contractSource.replaceAll("\r\n", "\n"))
            a <- parseScriptArgs(contractArgs).map { xs =>
              xs.map { case (tag, bv) => if (tag != "_") Proof(bv, Some(tag)) else Proof(bv) }
            }.toTry
          } yield ParsedInput(i, Some(c -> a))
        case _ => Failure(new RuntimeException("Inputs can not be parsed"))
      }
      .foldLeft(Seq.empty[ParsedInput].asRight[Throwable]) { (acc, e) =>
        e match {
          case Success(v) => acc.map(_ :+ v)
          case Failure(f) => f.asLeft
        }
      }
  }

  def parseScriptArgs(s: String): Either[ParseError[Char, String], Seq[(String, BoxedValue)]] = {

    val base16: Parser[List[Byte], Char, String] = Expressions.BASE16STRING
      .flatMap(x => Algos.decode(x).fold(_ => Fail, xs => PassWith(xs.toList)))
    val base58: Parser[List[Byte], Char, String] = Expressions.BASE58STRING
      .flatMap(x => Base58.decode(x).fold(_ => Fail, xs => PassWith(xs.toList)))

    val intValueExp: Parser[IntValue, Char, String] = P("IntValue(" ~ Lexer.integer ~ ")").map(IntValue)
    val byteValueExp: Parser[ByteValue, Char, String] =
      P("ByteValue(" ~ Lexer.integer.map(_.toByte) ~ ")").map(ByteValue)
    val boolValueExp: Parser[BoolValue, Char, String] =
      P("BoolValue(" ~ ("true" | "false").rep(min = 1, max = 1).!.map(_.toBoolean) ~ ")").map(BoolValue)
    val stringValueExp: Parser[StringValue, Char, String] =
      P("StringValue(" ~ Lexer.stringliteral ~ ")").map(StringValue)
    val byteCollectionValueExp: Parser[ByteCollectionValue, Char, String] =
      P("ByteCollectionValue(" ~ (base16 | base58) ~ ")").map(ByteCollectionValue)
    val signature25519ValueExp: Parser[Signature25519Value, Char, String] =
      P("Signature25519Value(" ~ base16 ~ ")").map(Signature25519Value)

    val boxedValue: Parser[BoxedValue, Char, String] =
      intValueExp | byteValueExp | boolValueExp | stringValueExp | byteCollectionValueExp | signature25519ValueExp

    val p: Parser[Seq[(String, BoxedValue)], Char, String] =
      (Lexer.identifier.map(_.name) ~ ":" ~ boxedValue).rep(sep = ";") ~ (";" | End)

    p.parse(s) match {
      case Parsed.Success(v, _) => Right(v)
      case f: Parsed.Failure[Char, String] => Left(new ParseError(f))
    }

  }
}