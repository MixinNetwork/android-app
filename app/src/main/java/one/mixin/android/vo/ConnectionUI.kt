package one.mixin.android.vo

data class ConnectionUI(
    val index: Int,
    val data: String,
    val name: String,
    val uri: String,
    val internalIcon: Int? = null,
    val icon: String? = null,
)
