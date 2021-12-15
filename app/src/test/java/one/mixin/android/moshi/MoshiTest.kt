package one.mixin.android.moshi

import androidx.collection.arrayMapOf
import com.squareup.moshi.Types
import okhttp3.ResponseBody.Companion.toResponseBody
import one.mixin.android.api.MixinResponse
import one.mixin.android.moshi.MoshiHelper.getTypeAdapter
import one.mixin.android.moshi.MoshiHelper.getTypeListAdapter
import one.mixin.android.vo.Asset
import one.mixin.android.vo.MentionUser
import one.mixin.android.vo.QuoteMessageItem
import one.mixin.android.vo.TranscriptMessage
import one.mixin.android.websocket.BlazeSignalKeyMessage
import org.junit.Test
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import retrofit2.Response
import kotlin.test.assertEquals

class MoshiTest {

    @Test
    fun transcriptTest() {
        val content =
            "[{\"content\":\"Test\",\"created_at\":\"2021-07-10T09:46:00.645Z\"," +
                "\"message_id\":\"0d47973b-a2a6-4ae8-9567-a606c1366d26\"," +
                "\"transcript_id\":\"2ac41ebb-3faa-45d2-ab15-a035b770c340\"," +
                "\"category\":\"SIGNAL_TEXT\",\"user_full_name\":\"宅学长\"," +
                "\"user_id\":\"639ec50a-d4f1-4135-8624-3c71189dcdcc\"}," +
                "{\"content\":\"9215ff4c-4857-465c-9f7c-94ce4d68e5e3\"," +
                "\"created_at\":\"2021-07-10T03:56:27.403Z\"," +
                "\"media_created_at\":\"2021-07-10T09:46:08.476167356Z\"," +
                "\"media_digest\":\"dL2/+cx1VD0xR4ZAQme2J61bYyrwCmBxSLdX4qU5eNU\\u003d\"," +
                "\"media_duration\":640," +
                "\"media_key\":\"U/a0CEm/IOnLHPRPywr4003MlAioMYlnx5uxV30zi9sxSygMVdWDHUwPzjV5" +
                "/b1Efc1iwtWRL0powMYDQTb2Ig\\u003d\\u003d\",\"media_mime_type\":\"audio/ogg\"," +
                "\"media_size\":1922," +
                "\"media_waveform\":\"AAAAAAAAAACQCSIAAEAAAAAQAAAgAABAAAAAAEAIIAAAAAAhAABAAACEEAAAIQQAQgABgAAAAAEAAEIAAAQA\",\"message_id\":\"40dc4a5c-91da-4e0c-82c7-ec7ebbd7992f\",\"transcript_id\":\"2ac41ebb-3faa-45d2-ab15-a035b770c340\",\"category\":\"SIGNAL_AUDIO\",\"user_full_name\":\"宅学长\",\"user_id\":\"639ec50a-d4f1-4135-8624-3c71189dcdcc\"}]"
        val jsonAdapter = getTypeListAdapter<List<TranscriptMessage>>(TranscriptMessage::class.java)
        val list = jsonAdapter.fromJson(content)
        print(list?.get(0)?.content)
        print(jsonAdapter.toJson(list))
    }

    @Test
    fun quoteMessageTest() {
        val content = """
            {"content":"test","conversationId":"3811aec7-c4a4-372c-a30f-acba92fd8045",
            "createdAt":"2019-09-25T07:48:08.530033Z",
            "messageId":"c649d782-b5b2-39ac-bbb9-f9e3021144f4","status":"DELIVERED",
            "type":"PLAIN_TEXT","userFullName":"宅学长",
            "userId":"639ec50a-d4f1-4135-8624-3c71189dcdcc","userIdentityNumber":"762532"}
        """
        val snakeContent = """
            {"content":"test","conversation_id":"3811aec7-c4a4-372c-a30f-acba92fd8045",
            "created_at":"2019-09-25T07:48:08.530033Z",
            "message_id":"c649d782-b5b2-39ac-bbb9-f9e3021144f4","status":"DELIVERED",
            "type":"PLAIN_TEXT","user_full_name":"宅学长",
            "userId":"639ec50a-d4f1-4135-8624-3c71189dcdcc","user_identity_number":"762532"}
        """
        val jsonAdapter = getTypeAdapter<QuoteMessageItem>(QuoteMessageItem::class.java)
        val json = jsonAdapter.fromJson(content)
        val jsonContent = jsonAdapter.toJson(json)
        println(jsonContent)
        println("----")
        val snakeJson = jsonAdapter.fromJson(snakeContent)
        val snakeJsonContent = jsonAdapter.toJson(snakeJson)
        println(snakeJsonContent)
        assertEquals(jsonContent, snakeJsonContent)
    }

    @Test
    fun mentionUserTest() {
        val content = """
            [{"full_name":"宅学长","identity_number":"26832"},{"full_name":"senior 😜",
            "identity_number":"37189829"},{"full_name":"马岱敲代码","identity_number":"35220"}]
        """
        val camelContent = """
            [{"fullName":"宅学长","identityNumber":"26832"},{"fullName":"senior 😜",
            "identityNumber":"37189829"},{"fullName":"马岱敲代码","identityNumber":"35220"}]
        """
        val jsonAdapter = getTypeListAdapter<List<MentionUser>>(MentionUser::class.java)
        val json = jsonAdapter.fromJson(content)
        val jsonContent = jsonAdapter.toJson(json)
        println(jsonContent)
        println("----")
        val camelJson = jsonAdapter.fromJson(camelContent)
        val camelJsonContent = jsonAdapter.toJson(camelJson)
        println(camelJsonContent)
        assertEquals(jsonContent, camelJsonContent)
    }

    @Test
    fun stringArrayTest() {
        val content = """
           ["a","b","c"] 
        """
        val listAdapter = getTypeListAdapter<List<String>>(String::class.java)
        val jsonList = listAdapter.fromJson(content)
        println(jsonList)
    }

    @Test
    fun listMapTest() {
        val map = arrayMapOf<String, String>()
        map["key1"] = "value1"
        map["key2"] = "value2"
        val map1 = arrayMapOf<String, String>()
        map1["key1"] = "value1"
        map1["key2"] = "value2"
        val list = listOf(map1, map)
        val jsonAdapter = getTypeListAdapter<List<Map<String, String>>>(Map::class.java)
        val json = jsonAdapter.toJson(list)
        println(json)
    }

    @Test
    fun iceCandidateTest() {
        val content = """
            {"adapterType":"UNKNOWN","sdp":"candidate:2029434911 1 udp 25108223 10.148.0.2 54932 typ relay raddr 0.0.0.0 rport 0 generation 0 ufrag hCP9 network-id 3 network-cost 10","sdpMLineIndex":0,"sdpMid":"0","serverUrl":"turn:35.240.137.101:443?transport\u003dtcp"}
        """
        val jsonAdapter = getTypeAdapter<IceCandidate>(IceCandidate::class.java)
        val iceCandidate = jsonAdapter.fromJson(content)
        println(jsonAdapter.toJson(iceCandidate))
        assertEquals(iceCandidate?.adapterType, PeerConnection.AdapterType.UNKNOWN)

        val arrayContent = """
           [{"adapterType":"UNKNOWN","sdp":"candidate:2029434911 1 udp 25108223 10.148.0.2 54932 typ relay raddr 0.0.0.0 rport 0 generation 0 ufrag hCP9 network-id 3 network-cost 10","sdpMLineIndex":0,"sdpMid":"0","serverUrl":"turn:35.240.137.101:443?transport\u003dtcp"},{"adapterType":"UNKNOWN","sdp":"candidate:2029434911 1 udp 25108223 10.148.0.2 54932 typ relay raddr 0.0.0.0 rport 0 generation 0 ufrag hCP9 network-id 3 network-cost 10","sdpMLineIndex":0,"sdpMid":"0","serverUrl":"turn:35.240.137.101:443?transport\u003dtcp"}]
        """
        val listJsonAdapter = getTypeListAdapter<List<IceCandidate>>(IceCandidate::class.java)
        val iceCandidates = listJsonAdapter.fromJson(arrayContent)
        println(listJsonAdapter.toJson(iceCandidates))
        assertEquals(iceCandidates?.size, 2)
    }

    @Test
    fun mapTest() {
        val map = arrayMapOf<String, Double>()
        map["1"] = 1.0
        map["2"] = 2.0
        val type = Types.newParameterizedType(Map::class.java, String::class.java, Double::class.javaObjectType)
        val jsonAdapter = getTypeAdapter<Map<String, Double>>(type)
        val mapString = jsonAdapter.toJson(map)
        val json = jsonAdapter.fromJson(mapString)!!
        assertEquals(json["1"], map["1"])
    }

    @Test
    fun testBlazeMessage() {
        val type = Types.newParameterizedType(List::class.java, BlazeSignalKeyMessage::class.java)
        val jsonAdapter = getTypeAdapter<List<BlazeSignalKeyMessage>>(type)
        println(jsonAdapter.toJson(null))
    }

    @Test
    fun testEmptyData() {
        val model = TestModel<Child?>("test title")
        val jsonAdapter = getTypeAdapter<TestModel<Child?>>(Types.newParameterizedType(TestModel::class.java, Child::class.java))
        println(jsonAdapter.toJson(model))
    }

    @Test
    fun testMixinResponse() {
        val response = MixinResponse<Any>(Response.error(400, "".toResponseBody()))
        val jsonAdapter = getTypeAdapter<MixinResponse<Any>>(Types.newParameterizedType(MixinResponse::class.java, String::class.java))
        val json = jsonAdapter.toJson(response)
        println(json)
        println(jsonAdapter.fromJson(json)?.errorCode)
    }

    @Test
    fun testArray() {
        val MOCK_ASSETS_JSON = """
    [
        {
          "type": "asset",
          "asset_id": "05891083-63d2-4f3d-bfbe-d14d7fb9b25a",
          "chain_id": "05891083-63d2-4f3d-bfbe-d14d7fb9b25a",
          "symbol": "BTS",
          "name": "BTS",
          "icon_url": "https://mixin-images.zeromesh.net/vPCw4G1BhBWLzFSVt8jMJxq7LhQgVRbn_IbgJif9mixgLyJfBTlrc4TbELTThAwQCdVqikJQNDDQ84nQZLVf1yGm=s128",
          "balance": "100",
          "destination": "mixin-bts",
          "tag": "556a9be0f8980077",
          "price_btc": "0.00000103",
          "price_usd": "0.01879055",
          "change_btc": "0",
          "change_usd": "0.03333863094062959",
          "asset_key": "1.3.0",
          "mixin_id": "",
          "reserve": "0",
          "confirmations": 64,
          "capitalization": 0,
          "liquidity": "0"
        },
        {
          "type": "asset",
          "asset_id": "05c5ac01-31f9-4a69-aa8a-ab796de1d041",
          "chain_id": "05c5ac01-31f9-4a69-aa8a-ab796de1d041",
          "symbol": "XMR",
          "name": "Monero",
          "icon_url": "https://mixin-images.zeromesh.net/vffCzX0PPO1f1D0sRFCkpJuSRYbxEM5u-hl4FMoUeWk8g899U5eyVKnFENiEJ4AXU0s-62mx1nBR3c_pHFROuw=s128",
          "balance": "100",
          "destination": "43x74R5LcxEUrEWBbWFSmyP5qRkGa5o4R3fFzXy2z93ses7gA4WcXdv4CS9FT5UspRcewdns7SPMrJ4HrYW1Due9MsE2JQc",
          "tag": "",
          "price_btc": "0.00663775",
          "price_usd": "120.95",
          "change_btc": "-0.01565403340495988",
          "change_usd": "0.01904119976409133",
          "asset_key": "05c5ac01-31f9-4a69-aa8a-ab796de1d041",
          "mixin_id": "",
          "reserve": "0",
          "confirmations": 32,
          "capitalization": 0,
          "liquidity": "0"
        },
        {
          "type": "asset",
          "asset_id": "2204c1ee-0ea2-4add-bb9a-b3719cfff93a",
          "chain_id": "2204c1ee-0ea2-4add-bb9a-b3719cfff93a",
          "symbol": "ETC",
          "name": "Ether Classic",
          "icon_url": "https://mixin-images.zeromesh.net/fM9wgyNyB3Uiopx2FRFxhr-sYrvXZtJ-uCpk975wGdpoehoA59rIU-BQ4s_6YFMDEthQ74KCPysOIWSFK4vUG_Y=s128",
          "balance": "100",
          "destination": "0xc64264dfFE9085729Ba8bE18E992421e5172be65",
          "tag": "",
          "price_btc": "0.00032379",
          "price_usd": "5.9",
          "change_btc": "-0.022461733538628747",
          "change_usd": "0.012006861063464836",
          "asset_key": "0x0000000000000000000000000000000000000000",
          "mixin_id": "",
          "reserve": "0",
          "confirmations": 100000,
          "capitalization": 0,
          "liquidity": "0"
        },
        {
          "type": "asset",
          "asset_id": "23dfb5a5-5d7b-48b6-905f-3970e3176e27",
          "chain_id": "23dfb5a5-5d7b-48b6-905f-3970e3176e27",
          "symbol": "XRP",
          "name": "Ripple",
          "icon_url": "https://mixin-images.zeromesh.net/SyX2tH2mBbSc45IfkOysbbd8WtPEjla5R3xT9ym0tbKv_vAyzl_Jd5qEYsOhKyuFRv09w3uB4Vzs2XJuJzZeO7e_=s128",
          "balance": "100",
          "destination": "rKmp1SR2dtdbySsgszaEQZDp1crBqrfgbQ",
          "tag": "",
          "price_btc": "0.00001655",
          "price_usd": "0.30153",
          "change_btc": "0.012851897184822521",
          "change_usd": "0.04855564094628382",
          "asset_key": "23dfb5a5-5d7b-48b6-905f-3970e3176e27",
          "mixin_id": "",
          "reserve": "20",
          "confirmations": 12,
          "capitalization": 0,
          "liquidity": "0"
        },
        {
          "type": "asset",
          "asset_id": "43d61dcd-e413-450d-80b8-101d5e903357",
          "chain_id": "43d61dcd-e413-450d-80b8-101d5e903357",
          "symbol": "ETH",
          "name": "Ether",
          "icon_url": "https://mixin-images.zeromesh.net/zVDjOxNTQvVsA8h2B4ZVxuHoCF3DJszufYKWpd9duXUSbSapoZadC7_13cnWBqg0EmwmRcKGbJaUpA8wFfpgZA=s128",
          "balance": "100",
          "destination": "0xB45c8eF628a5BDC843C1c2Cdb52e4723864370e7",
          "tag": "",
          "price_btc": "0.02727927",
          "price_usd": "497.07",
          "change_btc": "0.02015572001810002",
          "change_usd": "0.05611269281434581",
          "asset_key": "0x0000000000000000000000000000000000000000",
          "mixin_id": "",
          "reserve": "0",
          "confirmations": 16,
          "capitalization": 0,
          "liquidity": "0"
        },
        {
          "type": "asset",
          "asset_id": "574388fd-b93f-4034-a682-01c2bc095d17",
          "chain_id": "574388fd-b93f-4034-a682-01c2bc095d17",
          "symbol": "BSV",
          "name": "Bitcoin SV",
          "icon_url": "https://mixin-images.zeromesh.net/1iUl5doLjMSv-ElcVCI4YgD1uIayDbZcQP0WjFEajoY1-qQZmVEl5GgUCtsp8CP0aj96a5Rwi-weQ5YA64lyQzU=s128",
          "balance": "100",
          "destination": "12jDBd7VLRwZCE7jQeFaxYHff8pXeTgzgg",
          "tag": "",
          "price_btc": "0.00901187",
          "price_usd": "164.21",
          "change_btc": "-0.0023944207671445176",
          "change_usd": "0.03276729559748428",
          "asset_key": "574388fd-b93f-4034-a682-01c2bc095d17",
          "mixin_id": "",
          "reserve": "0",
          "confirmations": 72,
          "capitalization": 0,
          "liquidity": "0"
        },
        {
          "type": "asset",
          "asset_id": "6cfe566e-4aad-470b-8c9a-2fd35b49c68d",
          "chain_id": "6cfe566e-4aad-470b-8c9a-2fd35b49c68d",
          "symbol": "EOS",
          "name": "EOS",
          "icon_url": "https://mixin-images.zeromesh.net/a5dtG-IAg2IO0Zm4HxqJoQjfz-5nf1HWZ0teCyOnReMd3pmB8oEdSAXWvFHt2AJkJj5YgfyceTACjGmXnI-VyRo=s128",
          "balance": "100",
          "destination": "eoswithmixin",
          "tag": "1e40616a71dc72b58606a79f5a95c3e2",
          "price_btc": "0.00014708",
          "price_usd": "2.68",
          "change_btc": "0.003411106562969027",
          "change_usd": "0.03875968992248062",
          "asset_key": "eosio.token:EOS",
          "mixin_id": "",
          "reserve": "0",
          "confirmations": 64,
          "capitalization": 0,
          "liquidity": "0"
        },
        {
          "type": "asset",
          "asset_id": "76c802a2-7c88-447f-a93e-c29c9e5dd9c8",
          "chain_id": "76c802a2-7c88-447f-a93e-c29c9e5dd9c8",
          "symbol": "LTC",
          "name": "Litecoin",
          "icon_url": "https://mixin-images.zeromesh.net/dLK5T9I4YFA094o6nn-qZ_TWLUtIrL0xtjxOyURaLoPcl94m0JKQhXQiOrC775LS9d8apDfLXVfbpDzGmWDf0CWJ=s128",
          "balance": "100",
          "destination": "LMqGL4wTPKUu8bZnfhZrihqFjaycxHmufk",
          "tag": "",
          "price_btc": "0.00444858",
          "price_usd": "81.06",
          "change_btc": "0.07614264772051033",
          "change_usd": "0.11407366684991754",
          "asset_key": "76c802a2-7c88-447f-a93e-c29c9e5dd9c8",
          "mixin_id": "",
          "reserve": "0",
          "confirmations": 32,
          "capitalization": 0,
          "liquidity": "0"
        },
        {
          "type": "asset",
          "asset_id": "815b0b1a-2764-3736-8faa-42d694fa620a",
          "chain_id": "c6d0c728-2624-429b-8e0d-d9d19b6592fa",
          "symbol": "USDT",
          "name": "Tether USD",
          "icon_url": "https://mixin-images.zeromesh.net/ndNBEpObYs7450U08oAOMnSEPzN66SL8Mh-f2pPWBDeWaKbXTPUIdrZph7yj8Z93Rl8uZ16m7Qjz-E-9JFKSsJ-F=s128",
          "balance": "100",
          "destination": "1KVFm1JxRjtfiTopxYVUbEYmiMYGjyH6Pq",
          "tag": "",
          "price_btc": "0.00005486",
          "price_usd": "1",
          "change_btc": "-0.034324942791762014",
          "change_usd": "0",
          "asset_key": "815b0b1a-2764-3736-8faa-42d694fa620a",
          "mixin_id": "",
          "reserve": "0",
          "confirmations": 3,
          "capitalization": 0,
          "liquidity": "0"
        },
        {
          "type": "asset",
          "asset_id": "965e5c6e-434c-3fa9-b780-c50f43cd955c",
          "chain_id": "43d61dcd-e413-450d-80b8-101d5e903357",
          "symbol": "CNB",
          "name": "Chui Niu Bi",
          "icon_url": "https://mixin-images.zeromesh.net/0sQY63dDMkWTURkJVjowWY6Le4ICjAFuu3ANVyZA4uI3UdkbuOT5fjJUT82ArNYmZvVcxDXyNjxoOv0TAYbQTNKS=s128",
          "balance": "100",
          "destination": "0xB45c8eF628a5BDC843C1c2Cdb52e4723864370e7",
          "tag": "",
          "price_btc": "0",
          "price_usd": "0",
          "change_btc": "0",
          "change_usd": "0",
          "asset_key": "0xec2a0550a2e4da2a027b3fc06f70ba15a94a6dac",
          "mixin_id": "",
          "reserve": "0",
          "confirmations": 16,
          "capitalization": 0,
          "liquidity": "0"
        },
        {
          "type": "asset",
          "asset_id": "c6d0c728-2624-429b-8e0d-d9d19b6592fa",
          "chain_id": "c6d0c728-2624-429b-8e0d-d9d19b6592fa",
          "symbol": "BTC",
          "name": "Bitcoin",
          "icon_url": "https://mixin-images.zeromesh.net/HvYGJsV5TGeZ-X9Ek3FEQohQZ3fE9LBEBGcOcn4c4BNHovP4fW4YB97Dg5LcXoQ1hUjMEgjbl1DPlKg1TW7kK6XP=s128",
          "balance": "100",
          "destination": "1KVFm1JxRjtfiTopxYVUbEYmiMYGjyH6Pq",
          "tag": "",
          "price_btc": "1",
          "price_usd": "18235.57",
          "change_btc": "0",
          "change_usd": "0.03604423574595978",
          "asset_key": "c6d0c728-2624-429b-8e0d-d9d19b6592fa",
          "mixin_id": "",
          "reserve": "0",
          "confirmations": 3,
          "capitalization": 0,
          "liquidity": "0"
        },
        {
          "type": "asset",
          "asset_id": "c94ac88f-4671-3976-b60a-09064f1811e8",
          "chain_id": "43d61dcd-e413-450d-80b8-101d5e903357",
          "symbol": "XIN",
          "name": "Mixin",
          "icon_url": "https://mixin-images.zeromesh.net/UasWtBZO0TZyLTLCFQjvE_UYekjC7eHCuT_9_52ZpzmCC-X-NPioVegng7Hfx0XmIUavZgz5UL-HIgPCBECc-Ws=s128",
          "balance": "100",
          "destination": "0xB45c8eF628a5BDC843C1c2Cdb52e4723864370e7",
          "tag": "",
          "price_btc": "0.00780944",
          "price_usd": "142.3",
          "change_btc": "-0.004669847873078021",
          "change_usd": "0.03041274438812455",
          "asset_key": "0xa974c709cfb4566686553a20790685a47aceaa33",
          "mixin_id": "",
          "reserve": "0",
          "confirmations": 16,
          "capitalization": 0,
          "liquidity": "0"
        },
        {
          "type": "asset",
          "asset_id": "fd11b6e3-0b87-41f1-a41f-f0e9b49e5bf0",
          "chain_id": "fd11b6e3-0b87-41f1-a41f-f0e9b49e5bf0",
          "symbol": "BCH",
          "name": "Bitcoin Cash",
          "icon_url": "https://mixin-images.zeromesh.net/tqt14x8iwkiCR_vIKIw6gAAVO8XpZH7ku7ZJYB5ArMRA6grN9M1oCI7kKt2QqBODJwr17sZxDCDTjXHOgIixzv6X=s128",
          "balance": "100",
          "destination": "1CoxNe8sVSxrx3VpdDXdxLjM6NnKA4dDeK",
          "tag": "",
          "price_btc": "0.01393187",
          "price_usd": "253.86",
          "change_btc": "0.0028909327937309375",
          "change_usd": "0.038239744795713876",
          "asset_key": "fd11b6e3-0b87-41f1-a41f-f0e9b49e5bf0",
          "mixin_id": "",
          "reserve": "0",
          "confirmations": 120,
          "capitalization": 0,
          "liquidity": "0"
        }
      ]
"""

        val jsonAdapter = getTypeListAdapter<List<Asset>>(
            Asset::class.java)
        val array = jsonAdapter.fromJson(MOCK_ASSETS_JSON)?.toTypedArray()
        println(array?.size)
    }
}
