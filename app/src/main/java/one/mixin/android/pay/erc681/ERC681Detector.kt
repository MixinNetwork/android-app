package one.mixin.android.pay.erc681

import one.mixin.android.pay.EthereumURI
import one.mixin.android.pay.erc831.ERC831
import one.mixin.android.pay.erc831.toERC831

fun ERC831.isERC681() = scheme == "ethereum" && (prefix == null || prefix == "pay")

fun EthereumURI.isERC681() = toERC831().isERC681()
