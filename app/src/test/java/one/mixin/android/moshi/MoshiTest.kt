package one.mixin.android.moshi

import one.mixin.android.util.MoshiHelper.getTypeListAdapter
import one.mixin.android.vo.TranscriptMessage
import org.junit.Test

class MoshiTest {

    @Test
    fun transcriptTest() {
        val content =
            "[{\"content\":\"Test\",\"created_at\":\"2021-07-10T09:46:00.645Z\",\"message_id\":\"0d47973b-a2a6-4ae8-9567-a606c1366d26\",\"transcript_id\":\"2ac41ebb-3faa-45d2-ab15-a035b770c340\",\"category\":\"SIGNAL_TEXT\",\"user_full_name\":\"宅学长\",\"user_id\":\"639ec50a-d4f1-4135-8624-3c71189dcdcc\"},{\"content\":\"9215ff4c-4857-465c-9f7c-94ce4d68e5e3\",\"created_at\":\"2021-07-10T03:56:27.403Z\",\"media_created_at\":\"2021-07-10T09:46:08.476167356Z\",\"media_digest\":\"dL2/+cx1VD0xR4ZAQme2J61bYyrwCmBxSLdX4qU5eNU\\u003d\",\"media_duration\":640,\"media_key\":\"U/a0CEm/IOnLHPRPywr4003MlAioMYlnx5uxV30zi9sxSygMVdWDHUwPzjV5/b1Efc1iwtWRL0powMYDQTb2Ig\\u003d\\u003d\",\"media_mime_type\":\"audio/ogg\",\"media_size\":1922,\"media_waveform\":\"AAAAAAAAAACQCSIAAEAAAAAQAAAgAABAAAAAAEAIIAAAAAAhAABAAACEEAAAIQQAQgABgAAAAAEAAEIAAAQA\",\"message_id\":\"40dc4a5c-91da-4e0c-82c7-ec7ebbd7992f\",\"transcript_id\":\"2ac41ebb-3faa-45d2-ab15-a035b770c340\",\"category\":\"SIGNAL_AUDIO\",\"user_full_name\":\"宅学长\",\"user_id\":\"639ec50a-d4f1-4135-8624-3c71189dcdcc\"}]"
        val jsonAdapter = getTypeListAdapter<List<TranscriptMessage>>(TranscriptMessage::class.java)
        val list = jsonAdapter.fromJson(content)
        print(list?.get(0)?.content)
        print(jsonAdapter.toJson(list))
    }
}
