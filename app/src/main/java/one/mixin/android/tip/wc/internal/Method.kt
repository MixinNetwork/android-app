package one.mixin.android.tip.wc.internal

sealed class Method(val name: String) {
    object ETHSign : Method("eth_sign")

    object ETHPersonalSign : Method("personal_sign")

    object ETHSignTypedData : Method("eth_signTypedData")

    object ETHSignTypedDataV4 : Method("eth_signTypedData_v4")

    object ETHSignTransaction : Method("eth_signTransaction")

    object ETHSendTransaction : Method("eth_sendTransaction")

    object SolanaSignTransaction : Method("solana_signTransaction")

    object SolanaSignMessage : Method("solana_signMessage")

    object BtcGetAccountAddresses : Method("getAccountAddresses")

    object BtcSendTransfer : Method("sendTransfer")

    object BtcSignMessage : Method("signMessage")
}

val evmSupportedMethods =
    listOf(
        Method.ETHSign.name,
        Method.ETHPersonalSign.name,
        Method.ETHSignTypedData.name,
        Method.ETHSignTypedDataV4.name,
        Method.ETHSignTransaction.name,
        Method.ETHSendTransaction.name,
    )
val solanaSupportedMethods =
    listOf(
        Method.SolanaSignMessage.name,
        Method.SolanaSignTransaction.name,
    )
val bitcoinSupportedMethods =
    listOf(
        Method.BtcGetAccountAddresses.name,
        Method.BtcSendTransfer.name,
        Method.BtcSignMessage.name,
    )

internal fun isSupportedMethodForChain(
    method: String,
    chainId: String?,
): Boolean {
    val chain = getChainByChainId(chainId)
    return when {
        chain == Chain.Bitcoin -> bitcoinSupportedMethods.contains(method)
        chain == Chain.Solana -> solanaSupportedMethods.contains(method)
        chain != null && evmChainList.contains(chain) -> evmSupportedMethods.contains(method)
        else -> false
    }
}
