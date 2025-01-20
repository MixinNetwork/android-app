package one.mixin.android.vo.route

enum class OrderState(val value: String) {
    CREATED("created"),
    PENDING("pending"),
    SUCCESS("success"),
    FAILED("failed"),
    REFUNDED("refunded")
}
