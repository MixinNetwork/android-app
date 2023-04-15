package one.mixin.android.util.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import one.mixin.android.crypto.Base64
import one.mixin.android.extension.base64Encode
object ByteArrayBase64Serializer : KSerializer<ByteArray> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlinx.serialization.ByteArrayBase64Serializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ByteArray) {
        return encoder.encodeString(value.base64Encode())
    }

    override fun deserialize(decoder: Decoder): ByteArray {
        return Base64.decode(decoder.decodeString())
    }
}
