package com.sunflowerthu.meshcourier.domain.crypto

import com.sunflowerthu.meshcourier.domain.models.NodeId

interface CryptoManager {

    fun hasOwnKeyPair(): Boolean

    fun generateOwnKeyPair()

    /** Возвращает байты публичного ключа для передачи через QR-код. */
    fun getPublicKeyEncoded(): ByteArray

    /** Сохраняет публичный ключ собеседника, полученный через QR-код. */
    suspend fun importPeerPublicKey(nodeId: NodeId, encodedKey: ByteArray)

    fun hasPeerKey(nodeId: NodeId): Boolean

    /** Шифрует payload для конкретного получателя. */
    fun encrypt(plaintext: ByteArray, recipientId: NodeId): ByteArray

    /** Расшифровывает payload, зашифрованный отправителем senderId. */
    fun decrypt(ciphertext: ByteArray, senderId: NodeId): ByteArray

    /** Загружает сохранённые ключи собеседников в память при старте приложения. */
    suspend fun loadPersistedKeys()

    /** Подписывает данные приватным ключом этого узла (ГОСТ Р 34.10-2012). */
    fun sign(data: ByteArray): ByteArray

    /** Проверяет подпись по сохранённому публичному ключу отправителя. */
    fun verify(data: ByteArray, signature: ByteArray, signerId: NodeId): Boolean

    /** для дедупликации пакетов. */
    fun hash(data: ByteArray): ByteArray
}
