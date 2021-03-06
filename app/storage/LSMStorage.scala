package storage

import java.io.File
import io.iohk.iodb.{ByteArrayWrapper, LSMStore}
import models.Wallet
import scorex.crypto.signatures.{PrivateKey, PublicKey}
import javax.inject.Singleton
import org.encryfoundation.common.crypto.PrivateKey25519

@Singleton
class LSMStorage {

  private val dir: File = new File("keys")
  dir.mkdirs()

  lazy val store: LSMStore = new LSMStore(dir, keepVersions = 0)

  def getWalletSecret(w: Wallet): PrivateKey25519 = store.get(Wallet.secretKey(w.pubKey)) match {
    case Some(ByteArrayWrapper(d)) => PrivateKey25519(PrivateKey @@ d, PublicKey @@ Wallet.provider.generatePublicKey(d))
    case _ => throw new Exception("Secret not found")
  }
}
