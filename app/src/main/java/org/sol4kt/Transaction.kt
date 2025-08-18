package org.sol4kt

import org.sol4k.Base58
import org.sol4k.Constants.SIGNATURE_LENGTH
import org.sol4k.Transaction

fun Transaction.addPlaceholderSignature() {
    this.addSignature(Base58.encode(ByteArray(SIGNATURE_LENGTH)))
}