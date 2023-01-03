package one.mixin.android.pay.erc681

import java.math.BigInteger

data class ERC681(
    var valid: Boolean = true,
    var prefix: String? = null,
    var address: String? = null,
    var scheme: String? = null,
    var chainId: BigInteger? = null,
    var value: BigInteger? = null,
    var gasPrice: BigInteger? = null,
    var gasLimit: BigInteger? = null,

    var function: String? = null,
    var functionParams: List<Pair<String, String>> = listOf(),
)
