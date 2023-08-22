package one.mixin.android.vo.checkout.serializer

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import one.mixin.android.SafeBox
import java.io.InputStream
import java.io.OutputStream

object SafeBoxSerializer : Serializer<SafeBox> {
    override val defaultValue: SafeBox = SafeBox.getDefaultInstance()

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun readFrom(input: InputStream): SafeBox {
        try {
            return SafeBox.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun writeTo(t: SafeBox, output: OutputStream) = t.writeTo(output)
}
