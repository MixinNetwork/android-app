package one.mixin.android.util

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import one.mixin.android.ui.transfer.vo.TransferCommandData
import org.junit.Test

class SerializationTest {

    @Test
    fun test() {
        val data = TransferCommandData(
            action = "test",
            ip = "127.0.0.1",
            port = 8080,
            secretKey = "test_secret_key",
            code = 200,
            total = 123456789L,
            userId = "test_user_id",
            progress = 50.0f,
            version = 2,
            deviceId = "test_device_id",
            platform = "test_platform"
        )

        val jsonString = Json.encodeToString(data)
        println(jsonString)

        val parsedData = Json.decodeFromString<TransferCommandData>(jsonString)
        println(parsedData)
    }

}