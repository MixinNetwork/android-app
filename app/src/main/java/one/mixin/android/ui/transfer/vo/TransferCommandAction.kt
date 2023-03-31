package one.mixin.android.ui.transfer.vo

enum class TransferCommandAction(val value: String) {
    PUSH("push"),
    PULL("pull"),
    CONNECT("connect"),
    START("start"),
    CLOSE("close"),
    FINISH("finish"),
}
