package one.mixin.android.tip.wc

import androidx.lifecycle.LiveData

class WalletConnectLiveData : LiveData<Boolean>() {
    var connected: Boolean = false
        get() {
            return WalletConnect.hasInit() && field
        }
        set(value) {
            if (field != value) {
                field = value
                setValue(value)
            }
        }
}

val walletConnectLiveData = WalletConnectLiveData()
