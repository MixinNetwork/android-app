package one.mixin.android.pay.erc831

fun String.isEthereumURLString() = startsWith("ethereum:")
fun ERC831.isERC831() = scheme == "ethereum"
