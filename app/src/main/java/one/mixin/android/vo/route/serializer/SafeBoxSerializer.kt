package one.mixin.android.vo.route.serializer

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import one.mixin.android.vo.SafeBox
import java.io.InputStream
import java.io.OutputStream

object SafeBoxSerializer : Serializer<SafeBox> {
    override val defaultValue: SafeBox = SafeBox(mutableListOf())

    override suspend fun readFrom(input: InputStream): SafeBox {
        return try {
            Json.decodeFromString(
                SafeBox.serializer(),
                input.readBytes().decodeToString(),
            )
        } catch (serialization: SerializationException) {
            throw CorruptionException("Unable to read UserPrefs", serialization)
        } catch (e:Exception){
            defaultValue
        }
    }

    override suspend fun writeTo(
        safeBox: SafeBox,
        output: OutputStream,
    ) {
        output.write(Json.encodeToString(SafeBox.serializer(), safeBox).encodeToByteArray())
    }
}
