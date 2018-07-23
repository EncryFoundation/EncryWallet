package services

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import io.iohk.iodb.{ByteArrayWrapper, LSMStore}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import play.api.test.Injecting
import scorex.crypto.encode.Base58
import scorex.crypto.signatures.PublicKey
import scorex.crypto.hash.Blake2b256
import crypto.PrivateKey25519
import models.box.AssetBox
import storage.LSMStorage
import models.{EncryProposition, Wallet, WalletInfo}

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
      ws.createNewWallet(None) shouldBe a[Success[_]]
      verify(store, never).update(anyLong, any[Iterable[ByteArrayWrapper]], any[Iterable[(ByteArrayWrapper, ByteArrayWrapper)]])
    }

    "create a Wallet from an arbitrary String" in forAll { s: String =>
      val store: LSMStore = mock[LSMStore]
      when(store.get(any)) thenReturn None
      val lsmStorage: LSMStorage = mock[LSMStorage]
      when(lsmStorage.store) thenReturn store
      val mockExplorerService: ExplorerService = mock[ExplorerService]
      val ws: WalletService = new WalletService()(inject[ExecutionContext], lsmStorage, mockExplorerService)
      ws.createNewWallet(Some(s)) shouldBe a[Success[_]]
      verify(store, times(1)).update(anyLong, any[Iterable[ByteArrayWrapper]], any[Iterable[(ByteArrayWrapper, ByteArrayWrapper)]])
    }

    "receive a Wallet from the store if it exists" in forAll { s: String =>
      val store: LSMStore = mock[LSMStore]
      when(store.get(any)) thenReturn Some(ByteArrayWrapper(sampleWallets.head.pubKey))
      val lsmStorage: LSMStorage = mock[LSMStorage]
      when(lsmStorage.store) thenReturn store
      val mockExplorerService: ExplorerService = mock[ExplorerService]
      val ws: WalletService = new WalletService()(inject[ExecutionContext], lsmStorage, mockExplorerService)
      ws.createNewWallet(Some(s)) shouldBe a[Success[_]]
      verify(store, never).update(anyLong, any[Iterable[ByteArrayWrapper]], any[Iterable[(ByteArrayWrapper, ByteArrayWrapper)]])
    }
  }

  "WalletService#restoreFromSecret" should {

    "fail if a non Base58 string is given" in forAll { s: String =>
      val lsmStorage: LSMStorage = mock[LSMStorage]
      val mockExplorerService: ExplorerService = mock[ExplorerService]
      val ws: WalletService = new WalletService()(inject[ExecutionContext], lsmStorage, mockExplorerService)
      ws.restoreFromSecret(s) shouldBe a[Failure[_]]
    }

    val privateKey: String = "4Etkd64NNYEDt8TZ21Z3jNHPvfbvEksmuuTwRUtPgqGH"
    val wallet: Wallet = Wallet(PublicKey @@ Base58.decode("9WMTsdbwsgdF9ZH8JdGsF5SnqcKy7fPSR4cift1iLPuw").get)

    "be added to the store if its not there" in {
      val store: LSMStore = mock[LSMStore]
      when(store.get(any)) thenReturn None
      val lsmStorage: LSMStorage = mock[LSMStorage]
      when(lsmStorage.store) thenReturn store
      val mockExplorerService: ExplorerService = mock[ExplorerService]
      val ws: WalletService = new WalletService()(inject[ExecutionContext], lsmStorage, mockExplorerService)
      val result: Try[Wallet] = ws.restoreFromSecret(privateKey)
      result shouldBe a[Success[_]]
      result.toOption.value shouldBe wallet
      verify(store, times(1)).update(anyLong, any[Iterable[ByteArrayWrapper]], any[Iterable[(ByteArrayWrapper, ByteArrayWrapper)]])
    }

    "be not be added to the store if its already there" in {
      val store: LSMStore = mock[LSMStore]
      when(store.get(any)) thenReturn Some(null)
      val lsmStorage: LSMStorage = mock[LSMStorage]
      when(lsmStorage.store) thenReturn store
      val mockExplorerService: ExplorerService = mock[ExplorerService]
      val ws: WalletService = new WalletService()(inject[ExecutionContext], lsmStorage, mockExplorerService)
      val result: Try[Wallet] = ws.restoreFromSecret(privateKey)
      result shouldBe a[Success[_]]
      result.toOption.value shouldBe wallet
      verify(store, never).update(anyLong, any[Iterable[ByteArrayWrapper]], any[Iterable[(ByteArrayWrapper, ByteArrayWrapper)]])
    }

  }

  "WalletService#loadAllWithInfo" should {

    val sampleProposition: EncryProposition = EncryProposition(new Array[Byte](0))
    val sampleBoxes: IndexedSeq[AssetBox] = IndexedSeq(AssetBox(sampleProposition, 0L, 1L, None), AssetBox(sampleProposition, 0L, 2L, None))
    val sampleWallets: List[Wallet] = createWallet() :: createWallet() :: Nil

    "make request to the explorer" in {
      val store: LSMStore = mock[LSMStore]
      when(store.get(any)) thenReturn Some(ByteArrayWrapper(sampleWallets.flatMap(_.pubKey).toArray))
      val lsmStorage: LSMStorage = mock[LSMStorage]
      when(lsmStorage.store) thenReturn store
      val mockExplorerService: ExplorerService = mock[ExplorerService]
      when(mockExplorerService.requestUtxos(anyString)) thenReturn Future.successful(sampleBoxes)
      val ws: WalletService = new WalletService()(inject[ExecutionContext], lsmStorage, mockExplorerService)
      ws.loadAllWithInfo().futureValue shouldBe sampleWallets.map(WalletInfo(_, 3L))
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
