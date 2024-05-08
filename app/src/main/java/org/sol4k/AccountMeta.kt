package org.sol4k

data class AccountMeta(
    val publicKey: PublicKey,
    val signer: Boolean = false,
    val writable: Boolean = false,
) {
    companion object {
        @JvmStatic
        fun signerAndWritable(publicKey: PublicKey): AccountMeta = AccountMeta(
            publicKey,
            signer = true,
            writable = true,
        )

        @JvmStatic
        fun signer(publicKey: PublicKey): AccountMeta = AccountMeta(
            publicKey,
            signer = true,
        )

        @JvmStatic
        fun writable(publicKey: PublicKey): AccountMeta = AccountMeta(
            publicKey,
            writable = true,
        )
    }
}
