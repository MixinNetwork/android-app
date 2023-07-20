package one.mixin.android.event

class MessageEvent(
    val action: MessageEventAction,
    val ids: List<String>,
)

enum class MessageEventAction {
    UPDATE,
    DELETE,
    INSERT,
}
