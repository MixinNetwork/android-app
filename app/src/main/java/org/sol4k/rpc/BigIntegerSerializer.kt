package org.sol4k.rpc

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind.STRING
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigInteger

internal object BigIntegerSerializer : KSerializer<BigInteger> {
    override fun deserialize(decoder: Decoder): BigInteger = decoder.decodeString().toBigInteger()

    override fun serialize(encoder: Encoder, value: BigInteger) = encoder.encodeString(value.toString())

    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("BigInteger", STRING)
}
