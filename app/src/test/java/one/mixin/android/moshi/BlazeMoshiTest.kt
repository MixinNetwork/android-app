package one.mixin.android.moshi

import com.squareup.moshi.Types
import one.mixin.android.websocket.BlazeMessage
import org.junit.Test

class BlazeMoshiTest {

    @Test
    fun testBlaze() {
        val src = """
            {"id":"479ae3a9-0abe-40f6-9744-fc16c4e95524","action":"LIST_KRAKEN_PEERS","data":{"peers":null}}
        """
        val blazeMessageType =
            Types.newParameterizedType(BlazeMessage::class.java, Any::class.java)
        val blazeMessage = MoshiHelper.getTypeAdapter<BlazeMessage<Any?>>(blazeMessageType).fromJson(src)
        println(blazeMessage?.id)
        println(MoshiHelper.getTypeAdapter<BlazeMessage<Any?>>(blazeMessageType).toJson(blazeMessage))
    }
}
