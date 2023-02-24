package one.mixin.android.tip.wc

abstract class WalletConnect {
    companion object {
        internal const val web3jTimeout = 3L
        internal const val defaultGasLimit = "250000"
    }

    enum class Version {
        V1, V2
    }
}
