package org.sol4k

import org.sol4k.Constants.SIGNATURE_LENGTH

fun Transaction.addPlaceholderSignature() {
    this.addSignature(Base58.encode(ByteArray(SIGNATURE_LENGTH)))
}