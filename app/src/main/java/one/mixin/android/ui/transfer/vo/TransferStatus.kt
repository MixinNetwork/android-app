package one.mixin.android.ui.transfer.vo

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
    PARSING,
    FINISHED,
}
