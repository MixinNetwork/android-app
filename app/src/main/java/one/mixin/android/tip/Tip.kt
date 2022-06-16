package one.mixin.android.tip

import kotlinx.coroutines.Dispatchers
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.response.TipSigner
import one.mixin.android.api.service.TipNodeService
import one.mixin.android.crypto.blst.aggregateSignatures
import one.mixin.android.crypto.blst.sign
import one.mixin.android.extension.decodeBase64
import one.mixin.android.extension.toHex
import java.security.MessageDigest
import javax.inject.Inject

class Tip @Inject internal constructor(private val tipNodeService: TipNodeService) {

    suspend fun genPriv(identityPub: String, ephemeral: String): String? {
        val tipConfig = handleMixinResponse(
            invokeNetwork = { tipNodeService.tipConfig() },
            switchContext = Dispatchers.IO,
            successBlock = {
                requireNotNull(it.data) { "Required tip config was null." }
            }
        ) ?: return null

        val identityPubBytes = identityPub.decodeBase64()
        val ephemeralBytes = ephemeral.decodeBase64()

        val nodeSeeds = getNodeSeeds(tipConfig.signers, identityPubBytes, ephemeralBytes)

        val nodeSigs = getNodeSigs(tipConfig.signers, nodeSeeds)

        val aggSig = aggregateSignatures(nodeSigs)
        return aggSig.compress().toHex()
    }

    private fun getNodeSeeds(tipSigners: List<TipSigner>, identityPub: ByteArray, ephemeral: ByteArray): List<ByteArray> {
        val sha256 = MessageDigest.getInstance("SHA256") // or other hash func
        val nodeSeeds = mutableListOf<ByteArray>()
        for (signer in tipSigners) {
            sha256.run {
                update(identityPub)
                update(ephemeral)
                update(signer.identity.toByteArray())
                nodeSeeds.add(digest())
            }
        }
        return nodeSeeds
    }

    private suspend fun getNodeSigs(tipSigners: List<TipSigner>, nodeSeeds: List<ByteArray>): List<String> {
        require(tipSigners.size == nodeSeeds.size) { "Required tipSigners size equals nodeSeeds size failed." }
        return emptyList()
    }

    private suspend fun fetchTipNodeSigns(tipSigner: TipSigner, nodeSeed: ByteArray): String {
        return ""
    }

    private fun genRequestSignature(signerIdentity: String, identityPub: String, nonce: String, grace: String): String {
        val m = identityPub.toByteArray() + signerIdentity.toByteArray() + nonce.toByteArray() + grace.toByteArray()
        val sig = sign(m, identityPub.decodeBase64())
        return sig.compress().toHex()
    }
}
