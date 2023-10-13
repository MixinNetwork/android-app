package one.mixin.android.event

data class CallEvent(val conversationId: String, val errorCode: Int, val action: String? = null)
