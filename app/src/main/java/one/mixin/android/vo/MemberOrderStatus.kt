package one.mixin.android.vo

enum class MemberOrderStatus(val value: String) {
    INITIAL("initial"),
    PAID("paid"),
    COMPLETED("completed"),
    CANCEL("cancel"),
    EXPIRED("expired"),
    FAILED("failed");

    companion object {
        fun fromString(value: String): MemberOrderStatus = entries.find { it.value == value } ?: INITIAL
    }
}
