package one.mixin.android.tip.wc.internal

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import one.mixin.android.util.decodeBase58
import java.lang.reflect.Type

class WcSolanaTransaction(
    val signatures: List<WcSignature>?,
    val feePayer: String?,
    val instructions: List<WcInstruction>?,
    val recentBlockhash: String?,
    val transaction: String,
)

class WcSolanaMessage(
    val pubkey: String,
    val message: String,
)

class WcSignature(
    val publicKey: String? = null,
    val signature: String?,
)

class WcInstruction(
    val keys: List<WcAccountMeta>,
    val programId: String,
    val data: List<Int>,
)

class WcAccountMeta(
    val pubkey: String,
    val isSigner: Boolean,
    val isWritable: Boolean,
)

class WcInstructionDeserializer : JsonDeserializer<WcInstruction> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): WcInstruction {
        val jsonObject = json.asJsonObject

        val keys = context.deserialize<List<WcAccountMeta>>(
            jsonObject.get("keys"),
            object : TypeToken<List<WcAccountMeta>>() {}.type
        )
        val programId = jsonObject.get("programId").asString

        val dataElement = jsonObject.get("data")
        val data = when {
            dataElement.isJsonArray -> {
                context.deserialize<List<Int>>(dataElement, object : TypeToken<List<Int>>() {}.type)
            }
            dataElement.isJsonPrimitive && dataElement.asJsonPrimitive.isString -> {
                dataElement.asString.decodeBase58().map { it.toUByte().toInt() }
            }
            else -> emptyList()
        }

        return WcInstruction(keys, programId, data)
    }
}
