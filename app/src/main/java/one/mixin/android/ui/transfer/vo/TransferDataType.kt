package one.mixin.android.ui.transfer.vo

enum class TransferDataType(val value: String) {
    COMMAND("command"),
    PARTICIPANT("participant"),
    CONVERSATION("conversation"),
    USER("user"),
    APP("App"),
    ASSET("asset"),
    SNAPSHOT("snapshot"),
    STICKER("sticker"),
    PIN_MESSAGE("pin_message"),
    TRANSCRIPT_MESSAGE("transcript_message"),
    MESSAGE("message"),
    MESSAGE_MENTION("message_mention"),
    EXPIRED_MESSAGE("expired_message"),
}
