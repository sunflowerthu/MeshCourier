import java.util.Date

enum class MessageStatus{
    PENDING,
    DELIVERED,
    PROCESSING
}

data class Message(
    val id : String,
    val senderNodeId : String,
    val receiverNodeId : String,
    val content : String,
    var ttl : Int = 128,
    var hopCount : Int = 0,
    val timestamp: Long = Date().time,
    val status : MessageStatus
) {
    fun isMyPacket(nodeId : String) : Boolean {
        return receiverNodeId == nodeId
    }

    fun forward() : Message {
        if (ttl <= 0){
            throw IllegalStateException()
        }

        return copy(
            ttl = ttl - 1,
            hopCount = hopCount + 1
        )
    }

}