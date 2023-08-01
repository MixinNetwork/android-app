package one.mixin.android.vo;

import com.google.gson.annotations.SerializedName

enum class BuyLimit {
    @SerializedName("unverified")
    UNVERIFIED,
    @SerializedName("verified")
    VERIFIED;

    val title: String
        get() {
            return when (this) {
                UNVERIFIED -> "Unverified"
                VERIFIED -> "Identity Verification"
            }
        }

    val limit: String
        get() {
            return when (this) {
                UNVERIFIED -> "$500 / week"
                VERIFIED -> "$5,000 / day"
            }
        }

    val description: String
        get() {
            return when (this) {
                UNVERIFIED -> "Buy a little crypto as gas fee or for web3 dapps by Google Pay."
                VERIFIED -> "Verify your identity to unlock more purchase restrictions"
            }
        }
}
