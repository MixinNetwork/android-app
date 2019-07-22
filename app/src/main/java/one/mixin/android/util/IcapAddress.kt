package one.mixin.android.util

import java.math.BigInteger

fun isIcapAddress(address: String): Boolean {
    return address.startsWith("iban:XE") || address.startsWith("IBAN:XE")
}

fun decodeICAP(s: String): String {
    // TODO: verify checksum and length
    val temp = s.substring(9)
    val index = if (temp.indexOf("?") > 0) temp.indexOf("?") else temp.length
    var address = BigInteger(temp.substring(0, index), 36).toString(16)
    while (address.length < 40)
        address = "0$address"
    return "0x$address"
}
