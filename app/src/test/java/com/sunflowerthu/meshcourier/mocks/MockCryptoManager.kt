package com.sunflowerthu.meshcourier.mocks

import com.sunflowerthu.meshcourier.domain.crypto.CryptoManager
import com.sunflowerthu.meshcourier.domain.models.NodeId

class MockCryptoManager : CryptoManager {
    var hasOwnKey = false
    val peerKeys = mutableMapOf<NodeId, ByteArray>()
    var signResult: ByteArray = ByteArray(64) { it.toByte() }
    var verifyResult = true
    var encryptTransform: (ByteArray) -> ByteArray = { it }
    var decryptTransform: (ByteArray) -> ByteArray = { it }
    var publicKeyBytes: ByteArray = ByteArray(32) { it.toByte() }

    var generateKeyPairError: Exception? = null

    override fun hasOwnKeyPair() = hasOwnKey
    override fun generateOwnKeyPair() {
        generateKeyPairError?.let { throw it }
        hasOwnKey = true
    }
    override fun getPublicKeyEncoded() = publicKeyBytes
    override suspend fun importPeerPublicKey(nodeId: NodeId, encodedKey: ByteArray) { peerKeys[nodeId] = encodedKey }
    override fun hasPeerKey(nodeId: NodeId) = nodeId in peerKeys
    override fun encrypt(plaintext: ByteArray, recipientId: NodeId) = encryptTransform(plaintext)
    override fun decrypt(ciphertext: ByteArray, senderId: NodeId) = decryptTransform(ciphertext)
    override suspend fun loadPersistedKeys() {}
    override fun sign(data: ByteArray) = signResult
    override fun verify(data: ByteArray, signature: ByteArray, signerId: NodeId) = verifyResult
    override fun hash(data: ByteArray) = data
}
