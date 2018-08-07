package services

import scala.concurrent.{ExecutionContext, Future}
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import scorex.crypto.encode.{Base16, Base58}
import scorex.crypto.signatures.{PrivateKey, PublicKey}
import storage.LSMStorage
import models._
import org.encryfoundation.common.crypto.PrivateKey25519
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import play.api.test.Injecting
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Span}

class TransactionServiceSpec extends PlaySpec with GuiceOneAppPerTest with Injecting with MockitoSugar with GeneratorDrivenPropertyChecks with ScalaFutures {

  val sampleWalletID: String = "7e627a119d6051f2930ee6651c6c52308b0f446144d19d2a179c2df3ec60565e"
  val sampleWalletPrivateKey: PrivateKey25519 = PrivateKey25519(
    PrivateKey @@ Base58.decode("4Etkd64NNYEDt8TZ21Z3jNHPvfbvEksmuuTwRUtPgqGH").get,
    PublicKey @@ Base16.decode(sampleWalletID).get
  )

  val sampleProposition: EncryProposition = EncryProposition(new Array[Byte](0))
  val sampleOutputId: String = "010000691b35d6eaae31a43a2327f58a78f47293a03715821cf83399e4e3a0b0"
  val sampleTxId: String = "0b6df74842f4088b8ba3b6ad7b744cd415769b4a27470f993699c3827a98030c"
  val sampleOutputs: Seq[Output] = Seq(Output(
    sampleOutputId,
    sampleTxId,
    100L,
    "487291c237b68dd2ab213be6b5d1174666074a5afab772b600ea14e8285affab",
    "ede3fb8cbace04e878a0207aeac9bd3ffb3754c84a25eaabe27d17e2493a0092",
    ""
  ))

  "TransactionService#sendPaymentTransaction" should {

    val samplePaymentTransactionRequest: PaymentTransactionRequest = PaymentTransactionRequest(1L, 2L, "4rsPPCu1p1UjjT45emepvCPen7t6qGeoNBg9ArBPGdxhTqn5dr")

    "fail if a not valid Base58 encoded string is given" in forAll { s: String =>
      val mockLSMStorage: LSMStorage = mock[LSMStorage]
      when(mockLSMStorage.getWalletSecret(any[Wallet])) thenReturn sampleWalletPrivateKey
      val mockExplorerService: ExplorerService = mock[ExplorerService]
      when(mockExplorerService.requestUtxos(anyString)) thenReturn Future.successful(sampleOutputs)
      when(mockExplorerService.commitTransaction(any[EncryTransaction])) thenReturn Future.successful(HttpResponse(StatusCodes.OK))
      val ts = new TransactionService()(inject[ExecutionContext], mockLSMStorage, mockExplorerService)
      ts.sendPaymentTransaction("true", samplePaymentTransactionRequest).failed.futureValue shouldBe an[IllegalArgumentException]
      verify(mockLSMStorage, never).getWalletSecret(any[Wallet])
      verify(mockExplorerService, never).requestUtxos(anyString)
      verify(mockExplorerService, never).commitTransaction(any[EncryTransaction])
    }

    "return response from explorer is wallet address is valid" in {
      val mockLSMStorage: LSMStorage = mock[LSMStorage]
      when(mockLSMStorage.getWalletSecret(any[Wallet])) thenReturn sampleWalletPrivateKey
      val mockExplorerService: ExplorerService = mock[ExplorerService]
      when(mockExplorerService.requestUtxos(anyString)) thenReturn Future.successful(sampleOutputs)
      when(mockExplorerService.commitTransaction(any[EncryTransaction])) thenReturn Future.successful(HttpResponse(StatusCodes.OK))
      val ts = new TransactionService()(inject[ExecutionContext], mockLSMStorage, mockExplorerService)
      ts.sendPaymentTransaction(sampleWalletID, samplePaymentTransactionRequest).futureValue.status shouldBe StatusCodes.OK
      verify(mockLSMStorage, times(1)).getWalletSecret(any[Wallet])
      verify(mockExplorerService, times(1)).requestUtxos(anyString)
      verify(mockExplorerService, times(1)).commitTransaction(any[EncryTransaction])
    }

  }

  "TransactionService#sendScriptedTransaction" should {

    val source: String =
      """
        |struct CustomerBox:Object(
        |  person:Object(name:String; age:Int);
        |  orders:Array[Object(product_id:Int; amount:Int;)];
        |  id:Int;
        |)
        |
        |contract (signature: Signature25519, tx: Transaction) = {
        |  let ownerPubKey = base58"GtBn7qJwK1v1EbB6CZdgmkcvt849VKVfWoJBMEWsvTew"
        |  checkSig(tx.messageToSign, ownerPubKey, signature)
        |}
      """.stripMargin

    val sampleScriptedTransactionRequest: ScriptedTransactionRequest = ScriptedTransactionRequest(1L, 2L, source)

    "fail if a not valid Base58 encoded string is given" in forAll { s: String =>
      val mockLSMStorage: LSMStorage = mock[LSMStorage]
      when(mockLSMStorage.getWalletSecret(any[Wallet])) thenReturn sampleWalletPrivateKey
      val mockExplorerService: ExplorerService = mock[ExplorerService]
      when(mockExplorerService.requestUtxos(anyString)) thenReturn Future.successful(sampleOutputs)
      when(mockExplorerService.commitTransaction(any[EncryTransaction])) thenReturn Future.successful(HttpResponse(StatusCodes.OK))
      val ts = new TransactionService()(inject[ExecutionContext], mockLSMStorage, mockExplorerService)
      ts.sendScriptedTransaction("true", sampleScriptedTransactionRequest).failed.futureValue shouldBe an[IllegalArgumentException]
      verify(mockLSMStorage, never).getWalletSecret(any[Wallet])
      verify(mockExplorerService, never).requestUtxos(anyString)
      verify(mockExplorerService, never).commitTransaction(any[EncryTransaction])
    }

    "return response from explorer if wallet address is valid" in {
      val mockLSMStorage: LSMStorage = mock[LSMStorage]
      when(mockLSMStorage.getWalletSecret(any[Wallet])) thenReturn sampleWalletPrivateKey
      val mockExplorerService: ExplorerService = mock[ExplorerService]
      when(mockExplorerService.requestUtxos(anyString)) thenReturn Future.successful(sampleOutputs)
      when(mockExplorerService.commitTransaction(any[EncryTransaction])) thenReturn Future.successful(HttpResponse(StatusCodes.OK))
      val ts = new TransactionService()(inject[ExecutionContext], mockLSMStorage, mockExplorerService)
      ts.sendScriptedTransaction(sampleWalletID, sampleScriptedTransactionRequest).futureValue(Timeout(Span(5000, Millis))).status shouldBe StatusCodes.OK
      verify(mockLSMStorage, times(1)).getWalletSecret(any[Wallet])
      verify(mockExplorerService, times(1)).requestUtxos(anyString)
      verify(mockExplorerService, times(1)).commitTransaction(any[EncryTransaction])
    }

    "fail if smart contract is not valid" in {
      val mockLSMStorage: LSMStorage = mock[LSMStorage]
      when(mockLSMStorage.getWalletSecret(any[Wallet])) thenReturn sampleWalletPrivateKey
      val mockExplorerService: ExplorerService = mock[ExplorerService]
      when(mockExplorerService.requestUtxos(anyString)) thenReturn Future.successful(sampleOutputs)
      when(mockExplorerService.commitTransaction(any[EncryTransaction])) thenReturn Future.successful(HttpResponse(StatusCodes.OK))
      val ts = new TransactionService()(inject[ExecutionContext], mockLSMStorage, mockExplorerService)
      ts.sendScriptedTransaction(sampleWalletID, sampleScriptedTransactionRequest.copy(source = "blablabla")).failed.futureValue shouldBe an[Exception]
      verify(mockLSMStorage, never).getWalletSecret(any[Wallet])
      verify(mockExplorerService, never).requestUtxos(anyString)
      verify(mockExplorerService, never).commitTransaction(any[EncryTransaction])
    }

  }

}

