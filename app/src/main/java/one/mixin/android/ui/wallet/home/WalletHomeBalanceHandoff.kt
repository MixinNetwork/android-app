package one.mixin.android.ui.wallet.home

object WalletHomeBalanceHandoff {
    private var privacyBalance: WalletHomeBalanceSnapshot? = null
    private val web3Balance = mutableMapOf<String, WalletHomeBalanceSnapshot>()

    @Synchronized
    fun savePrivacyBalance(snapshot: WalletHomeBalanceSnapshot) {
        privacyBalance = snapshot
    }

    @Synchronized
    fun consumePrivacyBalance(): WalletHomeBalanceSnapshot? {
        val snapshot = privacyBalance
        privacyBalance = null
        return snapshot
    }

    @Synchronized
    fun saveWeb3Balance(
        walletId: String,
        snapshot: WalletHomeBalanceSnapshot,
    ) {
        if (walletId.isNotEmpty()) {
            web3Balance[walletId] = snapshot
        }
    }

    @Synchronized
    fun consumeWeb3Balance(walletId: String): WalletHomeBalanceSnapshot? = web3Balance.remove(walletId)

    @Synchronized
    fun clear() {
        privacyBalance = null
        web3Balance.clear()
    }
}
