package one.mixin.android.moshi

import androidx.collection.arrayMapOf
import com.squareup.moshi.Types
import one.mixin.android.moshi.MoshiHelper.getTypeAdapter
import one.mixin.android.moshi.MoshiHelper.getTypeListAdapter
import one.mixin.android.vo.MentionUser
import one.mixin.android.vo.QuoteMessageItem
import one.mixin.android.vo.TranscriptMessage
import one.mixin.android.websocket.BlazeMessage
import one.mixin.android.websocket.BlazeSignalKeyMessage
import org.junit.Test
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
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
    fun blazeMessage() {
        val src = """
           {"id":"9194f3bf-edd2-4761-bba6-acd23c06b926","action":"LIST_PENDING_MESSAGES"} 
        """
        val blazeMessageType =
            Types.newParameterizedType(BlazeMessage::class.java, String::class.java)
        val jsonAdapter = getTypeAdapter<BlazeMessage<String?>>(blazeMessageType)
        val blazeMessage = jsonAdapter.fromJson(src)
        println("id:${blazeMessage?.id}")
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
    fun testBlazeMessage(){
        val type = Types.newParameterizedType(List::class.java, BlazeSignalKeyMessage::class.java)
        val jsonAdapter = getTypeAdapter<List<BlazeSignalKeyMessage>>(type)
        println(jsonAdapter.toJson(null))
    }

    @Test
    fun testEmptyData(){
        val model = TestModel<Child?>("test title")
        val jsonAdapter = getTypeAdapter<TestModel<Child?>>(Types.newParameterizedType(TestModel::class.java, Child::class.java))
        println(jsonAdapter.toJson(model))
    }
}
