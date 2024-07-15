package one.mixin.android.event

data class TokenEvent(val chainId: String, val address: String) {
    val tokenId: String
        get() {
            return chainId + address
        }
}