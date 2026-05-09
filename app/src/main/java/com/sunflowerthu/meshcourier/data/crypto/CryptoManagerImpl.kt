package com.sunflowerthu.meshcourier.data.crypto

import com.sunflowerthu.meshcourier.domain.crypto.CryptoManager
import com.sunflowerthu.meshcourier.domain.repository.PeerKeyRepository
import com.sunflowerthu.meshcourier.domain.models.NodeId
import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.ContentSigner
import java.io.OutputStream
import ru.CryptoPro.JCSP.JCSP
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Signature
import java.security.cert.X509Certificate
import java.security.spec.X509EncodedKeySpec
import java.util.Date
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptoManagerImpl @Inject constructor(
    private val peerKeyRepository: PeerKeyRepository
) : CryptoManager {

    companion object {
        private const val KEY_STORE_TYPE = "HDIMAGE"
        private const val OWN_KEY_ALIAS = "meshcourier_node_key"

        private const val KEY_ALGORITHM = "GOST3410_2012_256"
        private const val SIGN_ALGORITHM = "GOST3411_2012_256withGOST3410_2012_256"
        private const val DIGEST_ALGORITHM = "GOST3411-2012-256"
        private const val KEY_AGREEMENT_ALGORITHM = "GOST3410_2012_256"
        private const val CIPHER_ALGORITHM = "GOST3412-2015/CTR-ACPKM/NoPadding"

        private const val NONCE_SIZE = 16
    }

    private val secureRandom = SecureRandom()

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(KEY_STORE_TYPE, JCSP.PROVIDER_NAME).also { it.load(null, null) }
    }

    @Volatile private var cachedPrivateKey: java.security.PrivateKey? = null

    private val peerPublicKeys = mutableMapOf<NodeId, PublicKey>()

    override suspend fun loadPersistedKeys() {
        val keyFactory = KeyFactory.getInstance(KEY_ALGORITHM, JCSP.PROVIDER_NAME)
        peerKeyRepository.loadAll().forEach { (nodeId, encodedKey) ->
            runCatching {
                val spec = X509EncodedKeySpec(encodedKey)
                peerPublicKeys[nodeId] = keyFactory.generatePublic(spec)
            }
        }
    }

    override fun hasOwnKeyPair(): Boolean =
        runCatching { keyStore.containsAlias(OWN_KEY_ALIAS) }.getOrDefault(false)

    override fun generateOwnKeyPair() {
        val kg = KeyPairGenerator.getInstance(KEY_ALGORITHM, JCSP.PROVIDER_NAME)
        val kp = kg.generateKeyPair()
        val cert = generateSelfSignedCert(kp)
        keyStore.setKeyEntry(OWN_KEY_ALIAS, kp.private, null, arrayOf(cert))
        cachedPrivateKey = kp.private
    }

    private fun generateSelfSignedCert(keyPair: KeyPair): X509Certificate {
        val subject = X500Name("CN=MeshCourierNode")
        val now = System.currentTimeMillis()
        val certBuilder = JcaX509v3CertificateBuilder(
            subject,
            BigInteger.valueOf(now),
            Date(now),
            Date(now + 10L * 365 * 24 * 3600 * 1000),
            subject,
            keyPair.public
        )

        val sigAlgId = AlgorithmIdentifier(ASN1ObjectIdentifier("1.2.643.7.1.1.3.2"))
        val sig = Signature.getInstance(SIGN_ALGORITHM, JCSP.PROVIDER_NAME)
        sig.initSign(keyPair.private)

        val contentSigner = object : ContentSigner {
            private val out = object : OutputStream() {
                override fun write(b: Int) = sig.update(b.toByte())
                override fun write(buf: ByteArray, off: Int, len: Int) = sig.update(buf, off, len)
                }
            override fun getAlgorithmIdentifier() = sigAlgId
            override fun getOutputStream(): OutputStream = out
            override fun getSignature(): ByteArray = sig.sign()
        }

        return JcaX509CertificateConverter().getCertificate(certBuilder.build(contentSigner))
    }

    private fun ownPrivateKey(): java.security.PrivateKey =
        cachedPrivateKey ?: (keyStore.getKey(OWN_KEY_ALIAS, null) as? java.security.PrivateKey
            ?: throw IllegalStateException("Own private key not found in keystore"))
            .also { cachedPrivateKey = it }

    override fun getPublicKeyEncoded(): ByteArray =
        (keyStore.getCertificate(OWN_KEY_ALIAS)
            ?: throw IllegalStateException("Own certificate not found in keystore"))
            .publicKey.encoded

    override suspend fun importPeerPublicKey(nodeId: NodeId, encodedKey: ByteArray) {
        val spec = X509EncodedKeySpec(encodedKey)
        val kf = KeyFactory.getInstance(KEY_ALGORITHM, JCSP.PROVIDER_NAME)
        val publicKey = kf.generatePublic(spec)
        peerPublicKeys[nodeId] = publicKey
        peerKeyRepository.save(nodeId, encodedKey)
    }

    override fun hasPeerKey(nodeId: NodeId): Boolean = nodeId in peerPublicKeys

    /**
     * Возвращает [nonce (16 байт)] + [шифртекст].
     * Каждое сообщение шифруется с уникальным случайным nonce
     */
    override fun encrypt(plaintext: ByteArray, recipientId: NodeId): ByteArray {
        val recipientPublicKey = peerPublicKeys[recipientId]
            ?: throw IllegalStateException("No public key for $recipientId")

        val secretKey = deriveKey(deriveSharedSecret(ownPrivateKey(), recipientPublicKey))

        val nonce = ByteArray(NONCE_SIZE).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance(CIPHER_ALGORITHM, JCSP.PROVIDER_NAME)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(nonce))

        return nonce + cipher.doFinal(plaintext)
    }

    /**
     * Ожидает на входе [nonce (16 байт)] + [шифртекст] — формат, который создаёт encrypt().
     */
    override fun decrypt(ciphertext: ByteArray, senderId: NodeId): ByteArray {
        require(ciphertext.size > NONCE_SIZE) { "Ciphertext too short to contain nonce" }

        val senderPublicKey = peerPublicKeys[senderId]
            ?: throw IllegalStateException("No public key for $senderId")

        val secretKey = deriveKey(deriveSharedSecret(ownPrivateKey(), senderPublicKey))

        val nonce = ciphertext.copyOf(NONCE_SIZE)
        val encrypted = ciphertext.copyOfRange(NONCE_SIZE, ciphertext.size)

        val cipher = Cipher.getInstance(CIPHER_ALGORITHM, JCSP.PROVIDER_NAME)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(nonce))
        return cipher.doFinal(encrypted)
    }

    override fun sign(data: ByteArray): ByteArray {
        val sn = Signature.getInstance(SIGN_ALGORITHM, JCSP.PROVIDER_NAME)
        sn.initSign(ownPrivateKey())
        sn.update(data)
        return sn.sign()
    }

    override fun verify(data: ByteArray, signature: ByteArray, signerId: NodeId): Boolean {
        val publicKey = peerPublicKeys[signerId]
            ?: throw IllegalStateException("No public key for $signerId")
        val sn = Signature.getInstance(SIGN_ALGORITHM, JCSP.PROVIDER_NAME)
        sn.initVerify(publicKey)
        sn.update(data)
        return sn.verify(signature)
    }

    override fun hash(data: ByteArray): ByteArray =
        MessageDigest.getInstance(DIGEST_ALGORITHM, JCSP.PROVIDER_NAME).digest(data)

    private fun deriveSharedSecret(
        privateKey: java.security.PrivateKey,
        publicKey: PublicKey
    ): ByteArray {
        val ka = KeyAgreement.getInstance(KEY_AGREEMENT_ALGORITHM, JCSP.PROVIDER_NAME)
        ka.init(privateKey)
        ka.doPhase(publicKey, true)
        return ka.generateSecret()
    }

    private fun deriveKey(sharedSecret: ByteArray): SecretKeySpec {
        val keyMaterial = MessageDigest.getInstance(DIGEST_ALGORITHM, JCSP.PROVIDER_NAME)
            .digest(sharedSecret)
        return SecretKeySpec(keyMaterial.copyOf(32), "GOST3412-2015")
    }
}
