package one.mixin.android.ui.conversation.holder

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.text.TextUtils
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatSystemBinding
import one.mixin.android.session.Session
import one.mixin.android.ui.conversation.adapter.MessageAdapter
import one.mixin.android.util.mention.MentionTextView
import one.mixin.android.vo.Account
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.create
import one.mixin.android.websocket.SystemConversationAction
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
@Config(application = SystemHolderTest.TestApplication::class, manifest = Config.NONE, sdk = [29])
class SystemHolderTest {
    class TestApplication : MixinApplication() {
        private var noticeResources: Resources? = null

        override fun onCreate() {
            noticeResources = NoticeResources(super.getResources())
            MixinApplication.appContext = this
        }

        override fun getResources(): Resources = noticeResources ?: super.getResources()
    }

    private class NoticeResources(base: Resources) : Resources(base.assets, base.displayMetrics, base.configuration) {
        override fun getText(id: Int): CharSequence = when (id) {
            R.string.You -> "self"
            R.string.set_disappearing_message_time_to_hint -> "enabled(%1\$s,%2\$s)"
            R.string.disable_disappearing_message_hint -> "disabled(%1\$s)"
            R.string.changed_disappearing_message_settings_hint -> "changed(%1\$s)"
            R.string.created_this_group -> "created(%1\$s)"
            R.string.Learn_More -> "learn"
            R.string.chat_not_support -> "unsupported(%1\$s)"
            R.string.chat_not_support_url -> "https://example.com/help"
            else -> super.getText(id)
        }

        override fun getString(id: Int): String = getText(id).toString()

        override fun getString(id: Int, vararg formatArgs: Any?): String = getString(id).format(*formatArgs)

        override fun getQuantityString(id: Int, quantity: Int): String = when (id) {
            R.plurals.time_interval_unit_day -> "day-unit"
            R.plurals.time_interval_unit_week -> "week-unit"
            R.plurals.time_interval_unit_month -> "month-unit"
            else -> super.getQuantityString(id, quantity)
        }

        override fun getIntArray(id: Int): IntArray =
            if (id == R.array.name_colors) intArrayOf(Color.BLACK) else super.getIntArray(id)

        override fun getColor(id: Int, theme: Theme?): Int = when (id) {
            R.color.disappearing_message_background -> Color.MAGENTA
            R.color.disappearing_message_disabled_background, R.color.bgGrayNight -> Color.GRAY
            R.color.textPrimary -> Color.BLACK
            R.color.textPrimaryNight -> Color.WHITE
            R.color.colorBlue -> Color.BLUE
            R.color.wallet_green -> Color.GREEN
            R.color.wallet_pink -> Color.RED
            R.color.wallet_text_gray -> Color.GRAY
            else -> super.getColor(id, theme)
        }
    }

    private lateinit var context: Context
    private lateinit var holder: SystemHolder
    private var previousAccount: Account? = null
    private var openedSettings = 0
    private var selected = 0
    private var longPressed = 0
    private val listener = object : MessageAdapter.OnItemListener() {
        override fun onDisappearingMessagesClick() {
            openedSettings++
        }

        override fun onSelect(isSelect: Boolean, messageItem: MessageItem, position: Int) {
            selected++
        }

        override fun onLongClick(messageItem: MessageItem, position: Int): Boolean {
            longPressed++
            return true
        }
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        previousAccount = ReflectionHelpers.getField(Session, "self")
        ReflectionHelpers.setField(Session, "self", Gson().fromJson("{\"user_id\":\"me\"}", Account::class.java))
        val root = FrameLayout(context)
        root.addView(MentionTextView(context, null).apply { id = R.id.chat_info })
        holder = SystemHolder(ItemChatSystemBinding.bind(root))
    }

    @After
    fun tearDown() {
        ReflectionHelpers.setField(Session, "self", previousAccount)
    }

    @Test
    fun enabledDisabledAndLegacyNoticesShowActorAndMatchingState() {
        holder.bind(message("86400", "me"), false, false, listener)
        assertEquals("enabled(self,1 day-unit)", holder.binding.chatInfo.text.toString())
        assertEquals(ContextCompat.getColor(context, R.color.disappearing_message_background), holder.binding.chatInfo.backgroundTintList?.defaultColor)
        assertEquals(Int.MAX_VALUE, holder.binding.chatInfo.maxLines)
        assertNull(holder.binding.chatInfo.ellipsize)

        holder.bind(message("5184000").copy(userFullName = "Crossle"), false, false, listener)
        assertEquals("enabled(Crossle,2 month-unit)", holder.binding.chatInfo.text.toString())

        holder.bind(message("0"), false, false, listener)
        assertEquals("disabled(Alice)", holder.binding.chatInfo.text.toString())
        assertEquals(ContextCompat.getColor(context, R.color.disappearing_message_disabled_background), holder.binding.chatInfo.backgroundTintList?.defaultColor)

        holder.bind(message(null), false, false, listener)
        assertEquals("changed(Alice)", holder.binding.chatInfo.text.toString())
    }

    @Test
    fun textAndRowOpenSettingsButSelectionModeStillSelects() {
        val message = message("604800")
        holder.bind(message, false, false, listener)
        holder.binding.chatInfo.performClick()
        holder.itemView.performClick()
        holder.binding.chatInfo.performLongClick()
        assertEquals(2, openedSettings)
        assertEquals(1, longPressed)

        holder.bind(message, true, false, listener)
        holder.binding.chatInfo.performClick()
        holder.itemView.performClick()
        holder.binding.chatInfo.performLongClick()
        assertEquals(2, openedSettings)
        assertEquals(3, selected)
        assertEquals(1, longPressed)
    }

    @Test
    fun recycledNoticesClearLinksAndDoNotKeepSettingsClicks() {
        holder.bind(message(null).copy(actionName = "UNSUPPORTED"), false, false, listener)
        assertNotNull(holder.binding.chatInfo.movementMethod)

        holder.bind(message("2592000"), false, false, listener)
        assertNull(holder.binding.chatInfo.movementMethod)
        holder.binding.chatInfo.performClick()
        assertEquals(1, openedSettings)

        holder.bind(message(null).copy(actionName = SystemConversationAction.CREATE.name), false, false, listener)
        assertFalse(holder.binding.chatInfo.isClickable)
        assertFalse(holder.binding.chatInfo.isLongClickable)
        assertEquals(2, holder.binding.chatInfo.maxLines)
        assertEquals(TextUtils.TruncateAt.END, holder.binding.chatInfo.ellipsize)
        assertNull(holder.binding.chatInfo.backgroundTintList)
        holder.binding.chatInfo.performClick()
        holder.itemView.performClick()
        assertEquals(1, openedSettings)
    }

    private fun message(content: String?, userId: String = "alice") =
        create(MessageCategory.SYSTEM_CONVERSATION.name, "2026-01-01T00:00:00Z").copy(
            userId = userId,
            userFullName = "Alice",
            actionName = SystemConversationAction.EXPIRE.name,
            content = content,
        )
}
