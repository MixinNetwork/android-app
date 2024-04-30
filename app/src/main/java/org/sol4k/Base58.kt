package org.sol4k

import org.sol4k.utilities.Base58 as Base58Encoding

object Base58 {
    @JvmStatic
    fun encode(input: ByteArray): String = Base58Encoding.encode(input)

    @JvmStatic
    fun decode(input: String): ByteArray = Base58Encoding.decode(input)
}
