package controllers

import akka.stream.Materializer
import akka.util.Timeout
import io.circe.parser.parse
import io.circe.syntax._
import io.circe.{Json, ParsingFailure}
import models.{Wallet, WalletInfo}
import org.encryfoundation.common.crypto.PrivateKey25519
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.EitherValues._
import org.scalatest.Matchers._
import org.scalatest._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting, NoMaterializer}
import scorex.crypto.hash.Blake2b256
import services.WalletService
import scala.concurrent.{ExecutionContext, Future}

class WalletControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting with MockitoSugar {

  def createWallet(seed: String = ""): Wallet = Wallet(PrivateKey25519.generateKeys(Blake2b256.hash(seed.getBytes()))._2.pubKeyBytes)

  def contentAsJson(of: Future[Result])(implicit timeout: Timeout, mat: Materializer = NoMaterializer): Either[ParsingFailure, Json] = parse(contentAsString(of)(timeout, mat))

  val sampleWallet: Wallet = createWallet()

  "WalletController#loadAll" should {
    def test(sampleWallets: Seq[Wallet]): Assertion = {
      val mockWalletService: WalletService = mock[WalletService]
      when(mockWalletService.loadAll) thenReturn sampleWallets
      val wc: WalletController = new WalletController()(inject[ExecutionContext], mockWalletService, stubControllerComponents())
      val result: Future[Result] = wc.getAll().apply(FakeRequest())
      status(result) shouldBe OK
      contentType(result).value shouldBe "application/json"
      contentAsJson(result).right.value shouldBe sampleWallets.asJson
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
      when(mockWalletService.createNewWallet(any[Option[Array[Byte]]])) thenReturn sampleWallet
      val wc: WalletController = new WalletController()(inject[ExecutionContext], mockWalletService, stubControllerComponents())
      val result: Future[Result] = wc.createNewWallet().apply(FakeRequest())
      status(result) shouldBe OK
      contentType(result).value shouldBe "application/json"
      contentAsJson(result).right.value shouldBe sampleWallet.asJson
    }

    "give bad request error if not a valid Base58 tring given" in {
      val mockWalletService: WalletService = mock[WalletService]
      when(mockWalletService.createNewWallet(any[Option[Array[Byte]]])) thenThrow new RuntimeException("Ooops! Something went wrong!")
      val wc: WalletController = new WalletController()(inject[ExecutionContext], mockWalletService, stubControllerComponents())
      val result: Future[Result] = wc.createNewWallet().apply(FakeRequest().withTextBody("blablabla"))
      verify(mockWalletService, never).createNewWallet(any[Option[Array[Byte]]])
      status(result) shouldBe BAD_REQUEST
    }

  }

  "WalletController#restoreFromSecret" should {
    "return a valid json" in {
      val mockWalletService: WalletService = mock[WalletService]
      when(mockWalletService.restoreFromSecret(any[Array[Byte]])) thenReturn sampleWallet
      val wc: WalletController = new WalletController()(inject[ExecutionContext], mockWalletService, stubControllerComponents())
      val result: Future[Result] = wc.restoreFromSecret().apply(FakeRequest().withFormUrlEncodedBody("secretKey" -> "9WMTsdbwsgdF9ZH8JdGsF5SnqcKy7fPSR4cift1iLPuw"))
      status(result) shouldBe OK
      contentType(result).value shouldBe "application/json"
      contentAsJson(result).right.value shouldBe sampleWallet.asJson
    }

    "give bad request error if key is not a valid Base58 string" in {
      val mockWalletService: WalletService = mock[WalletService]
      when(mockWalletService.restoreFromSecret(any[Array[Byte]])) thenThrow new RuntimeException("Oops! Something went wrong!")
      val wc: WalletController = new WalletController()(inject[ExecutionContext], mockWalletService, stubControllerComponents())
      val result: Future[Result] = wc.restoreFromSecret().apply(FakeRequest())
      verify(mockWalletService, never).restoreFromSecret(any[Array[Byte]])
      status(result) shouldBe BAD_REQUEST
    }

    "give bad request on request without a query parameter" in {
      val mockWalletService: WalletService = mock[WalletService]
      val wc: WalletController = new WalletController()(inject[ExecutionContext], mockWalletService, stubControllerComponents())
      val result: Future[Result] = wc.restoreFromSecret().apply(FakeRequest())
      status(result) shouldBe BAD_REQUEST
    }

  }

  "WalletController#getAllWithInfo" should {

    val sampleWallets: List[Wallet] = createWallet() :: createWallet() :: Nil

    "make request to the explorer" in {
      val mockWalletService: WalletService = mock[WalletService]
      when(mockWalletService.loadAllWithInfo()) thenReturn Future.successful(sampleWallets.map(WalletInfo(_, 3L)))
      val wc: WalletController = new WalletController()(inject[ExecutionContext], mockWalletService, stubControllerComponents())
      val result: Future[Result] = wc.getAllWithInfo().apply(FakeRequest())
      status(result) shouldBe OK
      contentAsJson(result).right.value shouldBe sampleWallets.map(WalletInfo(_, 3L)).asJson
    }

    "handle error from request to the explorer" in {
      val mockWalletService: WalletService = mock[WalletService]
      when(mockWalletService.loadAllWithInfo()) thenReturn Future.failed(new RuntimeException("Oops! Something went wrong!"))
      val wc: WalletController = new WalletController()(inject[ExecutionContext], mockWalletService, stubControllerComponents())
      val result: Future[Result] = wc.getAllWithInfo().apply(FakeRequest())
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

  }

}
