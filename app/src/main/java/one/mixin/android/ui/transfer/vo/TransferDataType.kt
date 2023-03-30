package one.mixin.android.ui.transfer.vo

enum class TransferDataType(val value: String) {
    MESSAGE("message"),
    USER("user"),
    CONVERSATION("conversation"),
    TRANSCRIPT_MESSAGE("transcript_message"),
    SNAPSHOT("snapshot"),
    STICKER("sticker"),
    ASSET("asset"),
    COMMAND("command")
}
