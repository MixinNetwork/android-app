package one.mixin.android.event

class MessageEvent(
    val conversationId: String,
    val action: MessageEventAction,
    val ids: List<String>,
)

enum class MessageEventAction {
    UPDATE,
    DELETE,
    INSERT,
    RELATIIONSHIP,
}
