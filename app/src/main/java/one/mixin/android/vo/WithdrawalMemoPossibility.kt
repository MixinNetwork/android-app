package one.mixin.android.vo

import kotlinx.serialization.SerialName

enum class WithdrawalMemoPossibility(val value: String) {
    @SerialName("negative")
    NEGATIVE("negative"),

    @SerialName("possible")
    POSSIBLE("possible"),

    @SerialName("positive")
    POSITIVE("positive"),
}
