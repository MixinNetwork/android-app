package one.mixin.android.event

import androidx.annotation.IntDef

/**
 * Event indicating that a wallet has been refreshed.
 * @param walletId The unique identifier of the wallet
 * @param type The operation type: RENAME, CREATE, DELETE, or OTHER
 */
data class WalletRefreshedEvent(
    val walletId: String,
    @param:WalletOperationType val type: Int
)

/**
 * Annotation for wallet operation types.
 * Allowed values: RENAME, CREATE, DELETE, OTHER.
 */
@IntDef(
    WalletOperationType.RENAME,
    WalletOperationType.CREATE,
    WalletOperationType.DELETE,
    WalletOperationType.OTHER
)
@Retention(AnnotationRetention.SOURCE)
annotation class WalletOperationType {
    companion object {
        const val RENAME = 0
        const val CREATE = 1
        const val DELETE = 2
        const val OTHER = 3
    }
}