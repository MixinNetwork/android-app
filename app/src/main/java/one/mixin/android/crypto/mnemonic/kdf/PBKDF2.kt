package one.mixin.android.crypto.mnemonic.kdf

import org.bouncycastle.crypto.Digest
import org.bouncycastle.crypto.PBEParametersGenerator.PKCS5PasswordToUTF8Bytes
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.digests.SHA512Digest
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator
import org.bouncycastle.crypto.params.KeyParameter

class PBKDF2 {
    fun derive(pass: ByteArray, salt: ByteArray?, iterations: Int = 2048, digestParams: DigestParams = DigestParams.Sha512): ByteArray {
        val gen = PKCS5S2ParametersGenerator(digestParams.toDigest())
        gen.init(pass, salt, iterations)
        return (gen.generateDerivedParameters(digestParams.keySize) as KeyParameter).key
    }

    fun derive(pass: CharArray, salt: ByteArray?, iterations: Int = 2048, digestParams: DigestParams = DigestParams.Sha512): ByteArray =
        derive(PKCS5PasswordToUTF8Bytes(pass), salt, iterations, digestParams)

    fun DigestParams.toDigest(): Digest =
        when (this) {
            DigestParams.Sha256 -> SHA256Digest()
            DigestParams.Sha512 -> SHA512Digest()
        }
}
