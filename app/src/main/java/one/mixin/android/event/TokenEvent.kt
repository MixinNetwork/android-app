package one.mixin.android.event

data class TokenEvent(val chain: String, val address: String) {
    val tokenId: String
        get() {
            return chain + address
        }
}