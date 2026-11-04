package one.mixin.android.ui.conversation.adapter

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import io.noties.markwon.Markwon
import one.mixin.android.ui.conversation.chat.CompressedList
import one.mixin.android.ui.conversation.holder.SystemHolderTest
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.create
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
@Config(application = SystemHolderTest.TestApplication::class, manifest = Config.NONE, sdk = [29])
class MessageAdapterTest {
    private val markwon by lazy { Markwon.create(ApplicationProvider.getApplicationContext<Context>()) }

    @Test
    fun everyConversationHasAFirstNoticeWithConsistentMessageOffsets() {
        for (isSecret in listOf(false, true)) {
            for ((isGroup, isBot) in listOf(true to false, false to false, false to true)) {
                for (messages in listOf(emptyList(), listOf(message("first"), message("second")))) {
                    val adapter = adapter(messages, isSecret, isGroup, isBot)
                    assertEquals(messages.size + 1, adapter.itemCount)
                    assertEquals(MessageAdapter.SECRET_TYPE, adapter.getItemViewType(0))
                    assertEquals(MessageCategory.SECRET.name, adapter.getItem(0)?.type)
                    messages.forEachIndexed { index, message ->
                        assertEquals(index + 1, adapter.layoutPosition(index))
                        assertEquals(message.messageId, adapter.getItem(index + 1)?.messageId)
                        assertEquals(MessageAdapter.TEXT_TYPE, adapter.getItemViewType(index + 1))
                    }
                    assertNull(adapter.getItem(-1))
                    assertNull(adapter.getItem(adapter.itemCount))
                }
            }
        }
    }

    @Test
    fun strangerRemainsLastAndInsertNotificationsSkipTheFirstNotice() {
        val adapter = adapter(emptyList(), isSecret = false, isGroup = false)
        adapter.hasBottomView = true
        val observer = RecordingObserver()
        adapter.registerAdapterDataObserver(observer)
        assertEquals(2, adapter.itemCount)
        assertEquals(MessageAdapter.SECRET_TYPE, adapter.getItemViewType(0))
        assertEquals(MessageAdapter.STRANGER_TYPE, adapter.getItemViewType(1))

        adapter.submitPrevious(listOf(message("first"), message("second")))
        assertEquals(listOf(Triple("inserted", 1, 2)), observer.events)
        assertEquals(4, adapter.itemCount)
        assertEquals("first", adapter.getItem(1)?.messageId)
        assertEquals("second", adapter.getItem(2)?.messageId)
        assertEquals(MessageAdapter.STRANGER_TYPE, adapter.getItemViewType(3))

        observer.events.clear()
        adapter.insert(listOf(message("third")), atBottom = true)
        assertEquals(listOf(Triple("changed", 2, 1), Triple("inserted", 3, 1)), observer.events)
        assertEquals(5, adapter.itemCount)
        assertEquals("third", adapter.getItem(3)?.messageId)
        assertEquals(MessageAdapter.STRANGER_TYPE, adapter.getItemViewType(4))
        adapter.unregisterAdapterDataObserver(observer)
    }

    @Test
    fun durationChangesOnlyRefreshTheFirstNoticeAndIgnoreUnchangedValues() {
        for (isSecret in listOf(false, true)) {
            val adapter = adapter(listOf(message("message")), isSecret)
            val observer = RecordingObserver()
            adapter.registerAdapterDataObserver(observer)
            assertEquals(0L, adapter.expireIn)
            adapter.expireIn = 0L
            assertEquals(emptyList(), observer.events)

            adapter.expireIn = 86_400L
            adapter.expireIn = 86_400L
            assertEquals(listOf(Triple("changed", 0, 1)), observer.events)
            adapter.expireIn = 0L
            assertEquals(listOf(Triple("changed", 0, 1), Triple("changed", 0, 1)), observer.events)
            assertEquals("message", adapter.getItem(1)?.messageId)
            adapter.unregisterAdapterDataObserver(observer)
        }
    }

    private fun adapter(
        messages: List<MessageItem>,
        isSecret: Boolean,
        isGroup: Boolean = true,
        isBot: Boolean = false,
    ) = MessageAdapter(
        data = CompressedList(messages),
        miniMarkwon = markwon,
        onItemListener = object : MessageAdapter.OnItemListener() {},
        previousPage = {},
        nextPage = {},
        onWindowTrimmed = {},
        isGroup = isGroup,
        unreadMessageId = null,
        isBot = isBot,
        isSecret = isSecret,
    )

    private fun message(id: String) = create(MessageCategory.SIGNAL_TEXT.name, "2026-01-01T00:00:00Z")
        .copy(messageId = id, content = "message")

    private class RecordingObserver : RecyclerView.AdapterDataObserver() {
        val events = mutableListOf<Triple<String, Int, Int>>()

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            events += Triple("inserted", positionStart, itemCount)
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            events += Triple("changed", positionStart, itemCount)
        }
    }
}
