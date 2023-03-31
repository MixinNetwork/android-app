package one.mixin.android.ui.transfer.vo

enum class TransferStatus {
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
