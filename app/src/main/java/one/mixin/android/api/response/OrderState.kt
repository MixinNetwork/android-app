package one.mixin.android.api.response

enum class OrderState(val value: String) {
    Preordered("preordered"),
    Insession("insession"),
    Initial("initial"),
    Declined("declined"),
    Pending("pending"),
    Review("review"),
    Failed("failed"),
    Transferred("transferred")
}
