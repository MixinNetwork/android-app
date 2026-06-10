package one.mixin.android.ui.wallet.home

import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.vo.safe.TokenItem

object WalletHomeTokenHandoff {
    private var privacyTokens: List<TokenItem> = emptyList()
    private val web3Tokens = mutableMapOf<String, List<Web3TokenItem>>()

    @Synchronized
    fun savePrivacyTokens(tokens: List<TokenItem>) {
        privacyTokens = tokens.toList()
    }

    @Synchronized
    fun consumePrivacyTokens(): List<TokenItem> {
        val tokens = privacyTokens
        privacyTokens = emptyList()
        return tokens
    }

    @Synchronized
    fun saveWeb3Tokens(
        walletId: String,
        tokens: List<Web3TokenItem>,
    ) {
        if (walletId.isNotEmpty()) {
            web3Tokens[walletId] = tokens.toList()
        }
    }

    @Synchronized
    fun consumeWeb3Tokens(walletId: String): List<Web3TokenItem> =
        web3Tokens.remove(walletId).orEmpty()

    @Synchronized
    fun clear() {
        privacyTokens = emptyList()
        web3Tokens.clear()
    }
}
