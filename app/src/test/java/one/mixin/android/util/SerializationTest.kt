package one.mixin.android.util

import kotlinx.serialization.builtins.LongAsStringSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import one.mixin.android.ui.transfer.vo.TransferCommand
import one.mixin.android.ui.transfer.vo.TransferData
import one.mixin.android.vo.Asset
import one.mixin.android.vo.TranscriptMessage
import one.mixin.android.vo.WithdrawalMemoPossibility
import org.junit.Test
import kotlin.test.assertEquals

class SerializationTest {

    @Test
    fun testTransferCommand() {
        val data = TransferCommand(
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

        val parsedData = Json.decodeFromString<TransferCommand>(jsonString)
        println(parsedData)
    }

    private val json by lazy {
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = false
        }
    }

    @Test
    fun testTranscriptMessage() {
        LongAsStringSerializer
        val content =
            """{"data":{"transcript_id":"464be61d-c837-4a7f-a102-3d5134ff4040","message_id":"fa928395-090a-4346-a854-d2bae96e0159","user_id":"cbb20923-9020-490a-b8f6-e816883c9c99","user_full_name":"Wu","category":"SIGNAL_TEXT","created_at":"2023-04-08T16:06:09.490Z","content":"android test","media_url":null,"media_name":null,"media_size":null,"media_width":null,"media_height":null,"media_mime_type":null,"media_duration":null,"media_status":"","media_waveform":null,"thumb_image":null,"thumb_url":null,"media_key":null,"media_digest":null,"media_created_at":null,"sticker_id":null,"shared_user_id":null,"mentions":null,"quote_id":null,"quote_content":null,"caption":null},"type":"transcript_message"}"""
        val transferData = json.decodeFromString<TransferData<JsonElement>>(content)
        val transcriptMessage = json.decodeFromJsonElement<TranscriptMessage>(transferData.data)
        println(transcriptMessage.messageId)
    }

    @Test()
    fun testAsset() {
        val content = """
           {"asset_id":"6cfe566e-4aad-470b-8c9a-2fd35b49c68d","chain_id":"6cfe566e-4aad-470b-8c9a-2fd35b49c68d","fee_asset_id":"6cfe566e-4aad-470b-8c9a-2fd35b49c68d","symbol":"EOS","name":"EOS","icon_url":"https://mixin-images.zeromesh.net/OTwqLjEwc6v0JutJc-1sYkh_juFOvVbFz26WvvwfLGdKq6ZtwAT-wKhX0k-5PsgOK_Pd9rCQjZfwMJmiNXCBzpHnjapBtkCqAVCTCg=s128","balance":"0.0","deposit_entries":[{"destination":"eoswithmixin","tag":"d8a03e2368b871f3ed94270767a35710","properties":null}],"destination":"eoswithmixin","tag":"d8a03e2368b871f3ed94270767a35710","price_btc":"0.0","price_usd":"0.0","change_btc":"0.0","change_usd":"0.0","asset_key":"eosio.token:EOS","precision":4,"mixin_id":"6ac4cbffda9952e7f0d924e4cfb6beb29d21854ac00bfbf749f086302d0f7e5d","reserve":"0","confirmations":64,"capitalization":0,"liquidity":"0","price_updated_at":"2023-07-28T06:17:40.032742007Z","withdrawal_memo_possibility":"positive"} 
        """
        val asset = json.decodeFromString<Asset>(content)
        println(json.encodeToString(asset))
        assertEquals(asset.withdrawalMemoPossibility, WithdrawalMemoPossibility.POSITIVE)
    }
}
