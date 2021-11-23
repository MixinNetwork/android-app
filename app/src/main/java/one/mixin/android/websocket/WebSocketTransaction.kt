package one.mixin.android.websocket

interface TransactionCallbackSuccess {
    fun <T> success(data: BlazeMessage<T>)
}

interface TransactionCallbackError {
    fun <T> error(data: BlazeMessage<T>?)
}

class WebSocketTransaction(
    val tid: String,
    val success: TransactionCallbackSuccess,
    val error: TransactionCallbackError
)
