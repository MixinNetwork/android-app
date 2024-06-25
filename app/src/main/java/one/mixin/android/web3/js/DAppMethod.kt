package one.mixin.android.web3.js

enum class DAppMethod {
    SIGNTRANSACTION,
    SIGNPERSONALMESSAGE,
    SIGNMESSAGE,
    SIGNTYPEDMESSAGE,
    ECRECOVER,
    REQUESTACCOUNTS,
    WATCHASSET,
    ADDETHEREUMCHAIN,
    SWITCHETHEREUMCHAIN,
    SIGNRAWTRANSACTION, // solana sign tx
    SINGIN, // solana sign-in
    UNKNOWN,
    ;

    companion object {
        fun fromValue(value: String): DAppMethod {
            return when (value) {
                "signTransaction" -> SIGNTRANSACTION
                "signPersonalMessage" -> SIGNPERSONALMESSAGE
                "signMessage" -> SIGNMESSAGE
                "signTypedMessage" -> SIGNTYPEDMESSAGE
                "eth_signTypedData_v4" -> SIGNTYPEDMESSAGE
                "ecRecover" -> ECRECOVER
                "requestAccounts" -> REQUESTACCOUNTS
                "watchAsset" -> WATCHASSET
                "addEthereumChain" -> ADDETHEREUMCHAIN
                "switchChain" -> SWITCHETHEREUMCHAIN
                "switchEthereumChain" -> SWITCHETHEREUMCHAIN
                "signRawTransaction" -> SIGNRAWTRANSACTION
                "signIn" -> SINGIN
                else -> UNKNOWN
            }
        }
    }
}
