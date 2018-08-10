package services

import scala.concurrent.{ExecutionContext, Future}
import io.iohk.iodb.{ByteArrayWrapper, LSMStore}
import org.encryfoundation.common.crypto.PrivateKey25519
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.test.Injecting
import scorex.util.encode.Base58
import scorex.crypto.hash.Blake2b256
import scorex.crypto.signatures.PublicKey
import storage.LSMStorage
import models.{Output, Wallet, WalletInfo}

class WalletServiceSpec extends PlaySpec with GuiceOneAppPerTest with Injecting with MockitoSugar with GeneratorDrivenPropertyChecks with ScalaFutures {

  def createWallet(seed: String = ""): Wallet = Wallet(PrivateKey25519.generateKeys(Blake2b256.hash(seed.getBytes()))._2.pubKeyBytes)

  val sampleWallets: List[Wallet] = createWallet() :: createWallet() :: Nil

  "WalletService#loadAll" should {

    "correctly decode byte arrays" in {
      val store: LSMStore = mock[LSMStore]
      when(store.get(any)) thenReturn Some(ByteArrayWrapper(sampleWallets.flatMap(_.pubKey).toArray))
      val lsmStorage: LSMStorage = mock[LSMStorage]
      when(lsmStorage.store) thenReturn store
      val mockExplorerService: ExplorerService = mock[ExplorerService]
      val ws: WalletService = new WalletService()(inject[ExecutionContext], lsmStorage, mockExplorerService)
      ws.loadAll should contain theSameElementsAs sampleWallets
    }

    "handle empty response" in {
      val store: LSMStore = mock[LSMStore]
      when(store.get(any)) thenReturn None
      val lsmStorage: LSMStorage = mock[LSMStorage]
      when(lsmStorage.store) thenReturn store
      val mockExplorerService: ExplorerService = mock[ExplorerService]
      val ws: WalletService = new WalletService()(inject[ExecutionContext], lsmStorage, mockExplorerService)
      ws.loadAll shouldBe empty
    }

  }

  "WalletService#createNewWallet" should {

    "create a Wallet without a seed" in {
      val store: LSMStore = mock[LSMStore]
      when(store.get(any)) thenReturn Some(null)
      val lsmStorage: LSMStorage = mock[LSMStorage]
      when(lsmStorage.store) thenReturn store
      val mockExplorerService: ExplorerService = mock[ExplorerService]
      val ws: WalletService = new WalletService()(inject[ExecutionContext], lsmStorage, mockExplorerService)
      ws.createNewWallet(None) shouldBe a[Wallet]
      verify(store, never).update(anyLong, any[Iterable[ByteArrayWrapper]], any[Iterable[(ByteArrayWrapper, ByteArrayWrapper)]])
    }

    "receive a Wallet from the store if it exists" in forAll { s: String =>
      val store: LSMStore = mock[LSMStore]
      when(store.get(any)) thenReturn Some(ByteArrayWrapper(sampleWallets.head.pubKey))
      val lsmStorage: LSMStorage = mock[LSMStorage]
      when(lsmStorage.store) thenReturn store
      val mockExplorerService: ExplorerService = mock[ExplorerService]
      val ws: WalletService = new WalletService()(inject[ExecutionContext], lsmStorage, mockExplorerService)
      ws.createNewWallet(Some(Base58.decode("9WMTsdbwsgdF9ZH8JdGsF5SnqcKy7fPSR4cift1iLPuw").get)) shouldBe a[Wallet]
      verify(store, never).update(anyLong, any[Iterable[ByteArrayWrapper]], any[Iterable[(ByteArrayWrapper, ByteArrayWrapper)]])
    }
  }

  "WalletService#restoreFromSecret" should {

    val privateKey: String = "4Etkd64NNYEDt8TZ21Z3jNHPvfbvEksmuuTwRUtPgqGH"
    val wallet: Wallet = Wallet(PublicKey @@ Base58.decode("9WMTsdbwsgdF9ZH8JdGsF5SnqcKy7fPSR4cift1iLPuw").get)

    "be added to the store if its not there" in {
      val store: LSMStore = mock[LSMStore]
      when(store.get(any)) thenReturn None
      val lsmStorage: LSMStorage = mock[LSMStorage]
      when(lsmStorage.store) thenReturn store
      val mockExplorerService: ExplorerService = mock[ExplorerService]
      val ws: WalletService = new WalletService()(inject[ExecutionContext], lsmStorage, mockExplorerService)
      val result: Wallet = ws.restoreFromSecret(Base58.decode(privateKey).get)
      result shouldBe wallet
      verify(store, times(1)).update(anyLong, any[Iterable[ByteArrayWrapper]], any[Iterable[(ByteArrayWrapper, ByteArrayWrapper)]])
    }

    "be not be added to the store if its already there" in {
      val store: LSMStore = mock[LSMStore]
      when(store.get(any)) thenReturn Some(null)
      val lsmStorage: LSMStorage = mock[LSMStorage]
      when(lsmStorage.store) thenReturn store
      val mockExplorerService: ExplorerService = mock[ExplorerService]
      val ws: WalletService = new WalletService()(inject[ExecutionContext], lsmStorage, mockExplorerService)
      val result: Wallet = ws.restoreFromSecret(Base58.decode(privateKey).get)
      result shouldBe wallet
      verify(store, never).update(anyLong, any[Iterable[ByteArrayWrapper]], any[Iterable[(ByteArrayWrapper, ByteArrayWrapper)]])
    }

  }

  "WalletService#loadAllWithInfo" should {

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
    val sampleWallets: List[Wallet] = createWallet() :: createWallet() :: Nil

    "make request to the explorer" in {
      val store: LSMStore = mock[LSMStore]
      when(store.get(any)) thenReturn Some(ByteArrayWrapper(sampleWallets.flatMap(_.pubKey).toArray))
      val lsmStorage: LSMStorage = mock[LSMStorage]
      when(lsmStorage.store) thenReturn store
      val mockExplorerService: ExplorerService = mock[ExplorerService]
      when(mockExplorerService.requestUtxos(anyString)) thenReturn Future.successful(sampleOutputs)
      val ws: WalletService = new WalletService()(inject[ExecutionContext], lsmStorage, mockExplorerService)
      ws.loadAllWithInfo().futureValue shouldBe sampleWallets.map(WalletInfo(_, 100L))
      verify(mockExplorerService, times(sampleWallets.size)).requestUtxos(anyString)
    }

    "handle error from request to the explorer" in {
      val store: LSMStore = mock[LSMStore]
      when(store.get(any)) thenReturn Some(ByteArrayWrapper(sampleWallets.flatMap(_.pubKey).toArray))
      val lsmStorage: LSMStorage = mock[LSMStorage]
      when(lsmStorage.store) thenReturn store
      val mockExplorerService: ExplorerService = mock[ExplorerService]
      when(mockExplorerService.requestUtxos(anyString)) thenReturn Future.failed(new RuntimeException("Oops! Something went wrong!"))
      val ws: WalletService = new WalletService()(inject[ExecutionContext], lsmStorage, mockExplorerService)
      ws.loadAllWithInfo().failed.futureValue shouldBe a[RuntimeException]
      verify(mockExplorerService, times(sampleWallets.size)).requestUtxos(anyString)
    }

  }

}
