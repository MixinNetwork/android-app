package one.mixin.android.ui.transfer.vo

enum class TransferState {
    INITIALIZING,
    CREATED,
    CONNECTING,
    WAITING_FOR_CONNECTION,
    WAITING_FOR_VERIFICATION,
    VERIFICATION_COMPLETED,
    SENDING,
    ERROR,
    FINISHED
}
