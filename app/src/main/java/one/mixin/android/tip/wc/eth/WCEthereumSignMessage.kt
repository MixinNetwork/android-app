package one.mixin.android.tip.wc.eth

data class WCEthereumSignMessage(
    val raw: List<String>,
    val type: WCSignType,
) {
    enum class WCSignType {
        MESSAGE, PERSONAL_MESSAGE, TYPED_MESSAGE
    }

    /**
     * Raw parameters will always be the message and the addess. Depending on the WCSignType,
     * those parameters can be swapped as description below:
     *
     *  - MESSAGE: `[address, data ]`
     *  - TYPED_MESSAGE: `[address, data]`
     *  - PERSONAL_MESSAGE: `[data, address]`
     *
     *  reference: https://docs.walletconnect.org/json-rpc/ethereum#eth_signtypeddata
     */
    val data get() = when (type) {
        WCSignType.MESSAGE -> raw[1]
        WCSignType.TYPED_MESSAGE -> raw[1]
        WCSignType.PERSONAL_MESSAGE -> raw[0]
    }
}
