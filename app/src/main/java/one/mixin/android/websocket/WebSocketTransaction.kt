package one.mixin.android.websocket

interface TransactionCallbackSuccess {
    fun success(data: BlazeMessage)
}

interface TransactionCallbackError {
    fun error(data: BlazeMessage?)
}

class WebSocketTransaction(
    val tid: String,
    val success: TransactionCallbackSuccess,
    val error: TransactionCallbackError
)
