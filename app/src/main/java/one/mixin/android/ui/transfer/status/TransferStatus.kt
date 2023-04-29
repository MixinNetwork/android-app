package one.mixin.android.ui.transfer.status

enum class TransferStatus {
    INITIALIZING,
    WAITING_MESSAGE,
    CREATED,
    CONNECTING,
    WAITING_FOR_CONNECTION,
    WAITING_FOR_VERIFICATION,
    VERIFICATION_COMPLETED,
    SYNCING,
    ERROR,
    FINISHED,
}
