package one.mixin.android.api.response

enum class OrderState(val value: String) {
    Initial("initial"),
    Created("created"),
    Paying("paying"),
    Success("success"),
    Failed("failed"),
}
