package one.mixin.android.vo.safe

enum class RawTransactionState {
    signed,  // Signed, pending broadcast
    spent    // Broadcast (terminal state)
}
