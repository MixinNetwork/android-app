package one.mixin.android.vo

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
@Serializable(with = WithdrawalMemoPossibilitySerializer::class)
enum class WithdrawalMemoPossibility(val value: String) {
    @SerialName("negative")
    NEGATIVE("negative"),

    @SerialName("possible")
    POSSIBLE("possible"),

    @SerialName("positive")
    POSITIVE("positive"),
}

@Serializer(forClass = WithdrawalMemoPossibility::class)
object WithdrawalMemoPossibilitySerializer : KSerializer<WithdrawalMemoPossibility> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("WithdrawalMemoPossibility", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: WithdrawalMemoPossibility) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): WithdrawalMemoPossibility {
        return try {
            val key = decoder.decodeString()
            WithdrawalMemoPossibility.valueOf(key)
        } catch (e: IllegalArgumentException) {
            WithdrawalMemoPossibility.POSSIBLE
        }
    }
}
