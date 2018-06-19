package org.encryfoundation.wallet.wallet

import java.io.File
import java.security.{AlgorithmParameters, SecureRandom}

import com.google.common.primitives.{Ints, Longs}
import io.iohk.iodb.{ByteArrayWrapper, LSMStore}
import javax.crypto._
import javax.crypto.spec.{IvParameterSpec, PBEKeySpec, SecretKeySpec}
import org.encryfoundation.wallet.crypto.PrivateKey25519
import scorex.crypto.hash.{Blake2b256, Blake2b512, Digest32, Digest64}
import scorex.crypto.signatures.{Curve25519, PrivateKey, PublicKey}
import scorex.utils.Random

/**
  * KeyKeeperStorage manages LMStore with private keys (Only Pk25519)
  *
  * @param store - KeyKeeperStorage storage
  * @param passwordOpt - password to unlock storage
  */
case class KeyManager(store: LSMStore,
                      passwordOpt: Option[Array[Byte]]) {
  /**
    * Generate private key from some string bytes
    * @param seed
    * @return Key pair based on seed and chain code
    */
  //TODO: add generateFrom mnemonic key
  def deriveKeysFromSeed(seed: Array[Byte]): (PrivateKey25519, Array[Byte]) = {
    val seedHashBytes: Digest64 = Blake2b512.hash(seed)
    val hashSeq: Seq[Array[Byte]] = seedHashBytes.sliding(32).toSeq
    val pair: (PrivateKey, PublicKey) = Curve25519.createKeyPair(hashSeq.head)
    PrivateKey25519(pair._1, pair._2) -> hashSeq(1)
  }

  /**
    * Generate next key based on previous key
    * @param prevKey
    * @return Key pair based on previous key and chain code
    */
  def deriveNextKey(prevKey: (PrivateKey25519, Array[Byte])): (PrivateKey25519, Array[Byte]) = {
    val prevKeyHash: Digest64 = Blake2b512.hash(prevKey._1.publicKeyBytes ++ prevKey._2)
    val hashSeq: Seq[Array[Byte]] = prevKeyHash.sliding(32).toSeq
    val pair: (PrivateKey, PublicKey) = Curve25519.createKeyPair(hashSeq.head)
    PrivateKey25519(pair._1, pair._2) -> hashSeq(1)
  }

  /**
    * get hash of  Keys sequence
    * @param keysSeq
    */
  def keysHash(keysSeq: Seq[PrivateKey25519]): Digest32 = Blake2b256.hash(keysSeq.foldLeft(Array[Byte]()) {
    case (currentHash,key) => currentHash ++ Blake2b256.hash(key.publicKeyBytes)
  })

  /**
    * Generate keys from seed and keysHash
    * @return Sequence of keys
    */
  private def getKeysWithChainCode: Seq[(PrivateKey25519, Array[Byte])] = {

    val keysQty: Int =
      store.get(new ByteArrayWrapper(Blake2b256.hash("count"))).map(d => Ints.fromByteArray(d.data)).getOrElse(0)

    (0 until keysQty).foldLeft(Seq[(PrivateKey25519, Array[Byte])]()) {
      case (seq, _) =>
        if (seq.nonEmpty) seq :+ deriveNextKey(seq.last._1, seq.last._2)
        else seq :+ deriveKeysFromSeed(store.get(new ByteArrayWrapper(Blake2b256.hash("seed"))).get.data)
    }
  }

  // TODO: Add an ability to select the key.
  def mainKey: PrivateKey25519 = keys.last

  def updateKey(key: ByteArrayWrapper, newValue: Array[Byte]): Unit = {
    //delete previous value
    store.update(
      new ByteArrayWrapper(Blake2b256.hash(newValue ++ Longs.toByteArray(System.currentTimeMillis()))), Seq(key), Seq()
    )
    //put new value
    store.update(
      new ByteArrayWrapper(Blake2b256.hash(Blake2b256.hash(newValue ++ Longs.toByteArray(System.currentTimeMillis())))), Seq(), Seq((key, new ByteArrayWrapper(newValue)))
    )
  }

  def isLocked: Boolean = (1: Byte) == store.get(KeyManager.lockKey).map(_.data).getOrElse(Array(0: Byte)).head

  def getKey(key: ByteArrayWrapper): Array[Byte] =
    store.get(key).map(_.data).getOrElse(Array[Byte](0))

  def keys: Seq[PrivateKey25519] = {
    if (!isLocked) {
      getKeysWithChainCode.foldLeft(Seq[PrivateKey25519]()) {
        case (seq, elem) => seq :+ elem._1
      }
    }else{
      unlock()
      val keys: Seq[PrivateKey25519] = getKeysWithChainCode.foldLeft(Seq[PrivateKey25519]()) {
        case (seq, elem) => seq :+ elem._1
      }
      lock()
      keys
    }

  }

  def createNewKey(): Unit = {
    val newKeysQty: Int = store.get(KeyManager.countKey).map( d => Ints.fromByteArray(d.data)).getOrElse(0) + 1
    updateKey(KeyManager.countKey, Ints.toByteArray(newKeysQty))
  }

  /** open KeyKeeperStorage and return set of keys inside store. If store dosn't exist or store was damaged return
    * only one key seq, which was generated from user-app password. */
  def unlock(key: Array[Byte] = passwordOpt.getOrElse(Array[Byte]())): Unit = {
    updateKey(KeyManager.seedKey, decryptAES(key))
    updateKey(KeyManager.lockKey, Array(KeyManager.unlockFlag))
  }

  /** Lock KeyKeeperStorage with GOST 34.12-2015 or AES */
  def lock(key: Array[Byte] = passwordOpt.getOrElse(Array[Byte]())): Unit = {
    val (encryptSeed, iv, salt) = encryptAES(key)
    updateKey(KeyManager.seedKey, encryptSeed)
    updateKey(KeyManager.ivKey, iv)
    updateKey(KeyManager.saltKey, salt)
    updateKey(KeyManager.lockKey, Array(KeyManager.lockFlag))
  }

  def generateSalt: Array[Byte] = {
    val random: SecureRandom = new SecureRandom()
    val bytes: Array[Byte] = new Array[Byte](256/8)
    random.nextBytes(bytes)
    bytes
  }

  /**
    * Encrypt seed with key
    * @param key
    * @return encrypted Seed and IV
    */
  def encryptAES(key: Array[Byte]): (Array[Byte], Array[Byte], Array[Byte]) = {

    val seed: Array[Byte] = store.get(KeyManager.seedKey).map(_.data).getOrElse(Array[Byte]())

    val saltBytes: Array[Byte] = generateSalt

    // Derive the key
    val factory: SecretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")

    val spec: PBEKeySpec = new PBEKeySpec(key.map(_.asInstanceOf[Char]), saltBytes, 1000, 128)

    val secretKey: SecretKey = factory.generateSecret(spec)
    val secret: SecretKeySpec = new SecretKeySpec(secretKey.getEncoded, "AES")

    //encrypt the message
    val cipher: Cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.ENCRYPT_MODE, secret)
    val params: AlgorithmParameters = cipher.getParameters
    val ivBytes = params.getParameterSpec(classOf[IvParameterSpec]).getIV
    val encryptedTextBytes: Array[Byte] = cipher.doFinal(seed)

    (encryptedTextBytes, ivBytes, saltBytes)
  }

  /**
    * Decrypt seed with key
    * @param key
    * @return decrypted Seed
    */
  def decryptAES(key: Array[Byte]): Array[Byte] = {


    val saltBytes: Array[Byte] = store.get(KeyManager.saltKey).map(_.data).getOrElse(Array[Byte]())
    val ivBytes: Array[Byte] = store.get(KeyManager.ivKey).map(_.data).getOrElse(Array[Byte]())
    val encryptedTextBytes: Array[Byte] = store.get(KeyManager.seedKey).map(_.data).getOrElse(Array[Byte]())

    // Derive the key
    val factory: SecretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val spec = new PBEKeySpec(key.map(_.asInstanceOf[Char]), saltBytes, 1000, 128)

    val secretKey: SecretKey = factory.generateSecret(spec)
    val secret = new SecretKeySpec(secretKey.getEncoded, "AES")

    val cipher: Cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(ivBytes))


    var decryptedTextBytes: Array[Byte] = Array[Byte](32)
    try
      decryptedTextBytes = cipher.doFinal(encryptedTextBytes)
    catch {
      case e: IllegalBlockSizeException =>
        e.printStackTrace()
      case e: BadPaddingException =>
        e.printStackTrace()
    }
    decryptedTextBytes
  }

  def initStorage(seed: Array[Byte]): Unit = {

    store.update(System.currentTimeMillis(),
      Seq(),
      Seq((KeyManager.seedKey, new ByteArrayWrapper(seed)),
        (KeyManager.ivKey, new ByteArrayWrapper(Random.randomBytes(0))),
        (KeyManager.saltKey, new ByteArrayWrapper(Random.randomBytes(0))),
        (KeyManager.lockKey, new ByteArrayWrapper(Array(0:Byte))),
        (KeyManager.countKey, new ByteArrayWrapper(Ints.toByteArray(1)))
      )
    )
  }
}

object KeyManager {

  val lockFlag: Byte = 0
  val unlockFlag: Byte = 1

  val seedKey = ByteArrayWrapper(Blake2b512.hash("seed"))

  val lockKey = ByteArrayWrapper(Blake2b512.hash("lock"))

  val ivKey = ByteArrayWrapper(Blake2b512.hash("iv"))

  val saltKey = ByteArrayWrapper(Blake2b512.hash("salt"))

  val countKey = ByteArrayWrapper(Blake2b512.hash("count"))

  def getKeysDir: File = new File(s"/keys")

  def readOrGenerate(password: Option[Array[Byte]] = Option(Array[Byte]()),
                     seed: Array[Byte] = Random.randomBytes()): KeyManager = {

    val dir = getKeysDir
    dir.mkdirs()

    val keysStore: LSMStore = new LSMStore(dir, keepVersions = 0)
    val keyManager: KeyManager = KeyManager(keysStore, password)

    if (keyManager.keys.isEmpty) keyManager.initStorage(seed)
    if (!keyManager.isLocked) keyManager.lock()

    keyManager
  }
}