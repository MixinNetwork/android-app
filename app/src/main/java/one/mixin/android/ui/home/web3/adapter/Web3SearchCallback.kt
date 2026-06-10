package one.mixin.android.ui.home.web3.adapter

import one.mixin.android.db.web3.vo.Web3TokenItem

interface Web3SearchCallback {
    fun onTokenClick(token: Web3TokenItem)
}
