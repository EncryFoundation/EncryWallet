package controllers

import akka.stream.Materializer
import akka.util.Timeout
import crypto.PrivateKey25519
import io.circe.{Json, ParsingFailure}
import io.circe.syntax._
import models.Wallet
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.{any, anyString}
import play.api.mvc.Result
import play.api.test.{FakeRequest, Injecting, NoMaterializer}
import scorex.crypto.hash.Blake2b256
import services.WalletService
import play.api.test.Helpers._
import org.scalatest.Matchers._
import io.circe.parser.parse
import org.scalatest._
import org.scalatest.EitherValues._

import scala.concurrent.Future
import scala.util.{Failure, Success}

class WalletControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting with MockitoSugar {

  def createWallet(seed: String = ""): Wallet = Wallet(PrivateKey25519.generateKeys(Blake2b256.hash(seed.getBytes()))._2.pubKeyBytes)

  def contentAsJson(of: Future[Result])(implicit timeout: Timeout, mat: Materializer = NoMaterializer): Either[ParsingFailure, Json] = parse(contentAsString(of)(timeout, mat))

  val sampleWallet: Wallet = createWallet()

  "WalletController#loadAll" should {

    def test(sampleWallets: Seq[Wallet]): Assertion = {
      val mockWalletService: WalletService = mock[WalletService]
      when(mockWalletService.loadAll) thenReturn sampleWallets

      val wc: WalletController = new WalletController(mockWalletService, stubControllerComponents())
      val result: Future[Result] = wc.getAll().apply(FakeRequest())
      status(result) mustEqual OK
      contentType(result) should contain("application/json")
      contentAsJson(result).right.value should be(sampleWallets.asJson)
    }

    "return all wallets in Json format" in {
      test(createWallet() :: createWallet() :: Nil)
    }

    "handle empty list" in {
      test(Nil)
    }
  }

  "WalletController#createWallet" should {

    "return valid json" in {
      val mockWalletService: WalletService = mock[WalletService]
      when(mockWalletService.createNewWallet(any[Option[String]])) thenReturn Success(sampleWallet)
      val wc: WalletController = new WalletController(mockWalletService, stubControllerComponents())
      val result: Future[Result] = wc.createNewWallet().apply(FakeRequest())
      status(result) mustEqual OK
      contentType(result) should contain("application/json")
      contentAsJson(result).right.value should be(sampleWallet.asJson)

    }

    "give bad request error on service failure" in {
      val mockWalletService: WalletService = mock[WalletService]
      when(mockWalletService.createNewWallet(any[Option[String]])) thenReturn Failure(new RuntimeException("Ooops! Something went wrong!"))
      val wc: WalletController = new WalletController(mockWalletService, stubControllerComponents())
      val result: Future[Result] = wc.createNewWallet().apply(FakeRequest())
      status(result) mustEqual INTERNAL_SERVER_ERROR
    }

  }

  "WalletController#restoreFromSecret" should {

    "return valid json" in {
      val mockWalletService: WalletService = mock[WalletService]
      when(mockWalletService.restoreFromSecret(anyString)) thenReturn Success(sampleWallet)
      val wc: WalletController = new WalletController(mockWalletService, stubControllerComponents())
      val result: Future[Result] = wc.restoreFromSecret().apply(FakeRequest().withTextBody("testString"))
      status(result) mustEqual OK
      contentType(result) should contain("application/json")
      contentAsJson(result).right.value should be(sampleWallet.asJson)
    }

    "give bad request error on service failure" in {
      val mockWalletService: WalletService = mock[WalletService]
      when(mockWalletService.restoreFromSecret(anyString)) thenReturn Failure(new RuntimeException("Ooops! Something went wrong!"))
      val wc: WalletController = new WalletController(mockWalletService, stubControllerComponents())
      val result: Future[Result] = wc.restoreFromSecret().apply(FakeRequest())
      status(result) mustEqual BAD_REQUEST
    }

    "give bad request on request without body" in {
      val mockWalletService: WalletService = mock[WalletService]
      val wc: WalletController = new WalletController(mockWalletService, stubControllerComponents())
      val result: Future[Result] = wc.restoreFromSecret().apply(FakeRequest())
      status(result) mustEqual BAD_REQUEST
    }

  }

}