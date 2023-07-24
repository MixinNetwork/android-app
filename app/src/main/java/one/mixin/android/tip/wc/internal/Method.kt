package one.mixin.android.tip.wc.internal

sealed class Method(val name: String) {
    object ETHSign : Method("eth_sign")
    object ETHPersonalSign : Method("personal_sign")
    object ETHSignTypedData : Method("eth_signTypedData")
    object ETHSignTypedDataV4 : Method("eth_signTypedData_v4")
    object ETHSignTransaction : Method("eth_signTransaction")
    object ETHSendTransaction : Method("eth_sendTransaction")
}

val supportedMethods = listOf(
    Method.ETHSign.name,
    Method.ETHPersonalSign.name,
    Method.ETHSignTypedData.name,
    Method.ETHSignTypedDataV4.name,
    Method.ETHSignTransaction.name,
    Method.ETHSendTransaction.name,
)
