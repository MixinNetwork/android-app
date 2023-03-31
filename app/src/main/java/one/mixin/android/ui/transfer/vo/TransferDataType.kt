package one.mixin.android.ui.transfer.vo

enum class TransferDataType(val value: String) {
    MESSAGE("message"),
    USER("user"),
    CONVERSATION("conversation"),
    PARTICIPANT("participant"),
    TRANSCRIPT_MESSAGE("transcript_message"),
    PIN_MESSAGE("pin_message"),
    EXPIRED_MESSAGE("expired_message"),
    SNAPSHOT("snapshot"),
    STICKER("sticker"),
    ASSET("asset"),
    COMMAND("command"),
}
