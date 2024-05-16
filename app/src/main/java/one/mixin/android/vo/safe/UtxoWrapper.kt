package one.mixin.android.vo.safe

import one.mixin.android.util.GsonHelper

data class UtxoWrapper(val outputs: List<Output>) {
    val keys: List<List<String>> by lazy { generateKeys() }
    val ids: List<String> by lazy { generateIds() }
    val lastOutput by lazy { outputs.last() }

    val formatKeys: String by lazy {
        GsonHelper.customGson.toJson(keys)
    }

    val input: ByteArray by lazy {
        GsonHelper.customGson.toJson(generateUtxos()).toByteArray()
    }

    val firstSequence by lazy {
        outputs.first().sequence
    }

    val inscriptionHash by lazy {
        if (outputs.size == 1)
            {
                outputs.first().inscriptionHash
            } else {
            null
        }
    }

    private fun generateKeys(): List<List<String>> {
        return outputs.map { it.keys }
    }

    private fun generateIds(): List<String> {
        return outputs.map { it.outputId }
    }

    private fun generateUtxos(): List<Utxo> {
        return outputs.map { output ->
            Utxo(
                hash = output.transactionHash,
                amount = output.amount,
                index = output.outputIndex,
            )
        }
    }
}
