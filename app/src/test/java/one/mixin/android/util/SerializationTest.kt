package one.mixin.android.util

import kotlinx.serialization.builtins.LongAsStringSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import one.mixin.android.ui.transfer.vo.TransferCommandData
import one.mixin.android.ui.transfer.vo.TransferSendData
import one.mixin.android.vo.TranscriptMessage
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
            platform = "test_platform",
        )

        val jsonString = Json.encodeToString(data)
        println(jsonString)

        val parsedData = Json.decodeFromString<TransferCommandData>(jsonString)
        println(parsedData)
    }

    private val json by lazy {
        Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = false }
    }

    @Test
    fun testA() {
        LongAsStringSerializer
        val content =
            """{"data":{"transcript_id":"464be61d-c837-4a7f-a102-3d5134ff4040","message_id":"fa928395-090a-4346-a854-d2bae96e0159","user_id":"cbb20923-9020-490a-b8f6-e816883c9c99","user_full_name":"Wu","category":"SIGNAL_TEXT","created_at":"2023-04-08T16:06:09.490Z","content":"android test","media_url":null,"media_name":null,"media_size":null,"media_width":null,"media_height":null,"media_mime_type":null,"media_duration":null,"media_status":"","media_waveform":null,"thumb_image":null,"thumb_url":null,"media_key":null,"media_digest":null,"media_created_at":null,"sticker_id":null,"shared_user_id":null,"mentions":null,"quote_id":null,"quote_content":null,"caption":null},"type":"transcript_message"}"""
        val transferData = json.decodeFromString<TransferSendData<JsonElement>>(content)
        val transcriptMessage = json.decodeFromJsonElement<TranscriptMessage>(transferData.data)
        println(transcriptMessage.messageId)
    }
}
