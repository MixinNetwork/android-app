package one.mixin.android.ui.conversation

import android.app.Activity
import android.app.Application
import android.os.Looper
import android.os.SystemClock
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import android.widget.TextView
import androidx.emoji2.bundled.BundledEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat
import one.mixin.android.MixinApplication
import one.mixin.android.ui.conversation.adapter.MessageAdapter
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.create
import one.mixin.android.widget.GestureMessageLayout
import one.mixin.android.widget.linktext.AutoLinkTextView
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.util.ReflectionHelpers
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, manifest = Config.NONE, sdk = [29])
@LooperMode(LooperMode.Mode.PAUSED)
class TextHolderLongPressTest {
    @Test
    fun longPressChangesSelectionOnceAfterRebinding() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup().visible()
        val activity = controller.get()
        MixinApplication.appContext = activity.applicationContext
        EmojiCompat.init(BundledEmojiCompatConfig(activity, Runnable::run))
        val root = FrameLayout(activity)
        val bubble = GestureMessageLayout(activity, null)
        val text = AutoLinkTextView(activity, null).apply { this.text = "Long press this message" }
        bubble.addView(text)
        bubble.addView(TextView(activity).apply { this.text = "12:00" })
        root.addView(bubble)
        val message = create(MessageCategory.PLAIN_TEXT.name)
        var selected = false
        var changes = 0
        lateinit var gesture: GestureDetector.SimpleOnGestureListener
        fun bind() {
            ReflectionHelpers.setField(gesture, "hasSelect", selected)
            ReflectionHelpers.setField(gesture, "isSelect", selected)
            val wasSelected = selected
            val onLongClick = View.OnLongClickListener {
                selected = !wasSelected
                changes++
                bind()
                true
            }
            root.setOnLongClickListener(onLongClick)
            text.setOnLongClickListener(onLongClick)
            bubble.setOnLongClickListener { root.performLongClick() }
        }
        val listener = object : MessageAdapter.OnItemListener() {
            override fun onSelect(isSelect: Boolean, messageItem: MessageItem, position: Int) {
                selected = isSelect
                changes++
                bind()
            }
        }
        val constructor = Class.forName("one.mixin.android.ui.conversation.holder.TextHolder\$TextGestureListener")
            .getDeclaredConstructor(
                View::class.java, MessageItem::class.java, Boolean::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType, MessageAdapter.OnItemListener::class.java, Int::class.javaPrimitiveType,
            )
        constructor.isAccessible = true
        gesture = constructor.newInstance(root, message, false, false, listener, 0) as GestureDetector.SimpleOnGestureListener
        bubble.listener = gesture
        bind()
        activity.setContentView(root)
        val looper = shadowOf(Looper.getMainLooper())
        looper.idle()
        try {
            repeat(2) { press ->
                val downTime = SystemClock.uptimeMillis()
                fun dispatch(action: Int) {
                    val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, text.x + 4f, text.y + 4f, 0)
                    bubble.dispatchTouchEvent(event)
                    event.recycle()
                }
                dispatch(MotionEvent.ACTION_DOWN)
                looper.idleFor(Duration.ofMillis(ViewConfiguration.getLongPressTimeout().toLong() + 200))
                dispatch(MotionEvent.ACTION_UP)
                looper.idleFor(Duration.ofMillis(ViewConfiguration.getDoubleTapTimeout().toLong() + 100))
                assertEquals("One selection change per long press", press + 1, changes)
                assertEquals(press == 0, selected)
            }
        } finally {
            controller.pause().stop().destroy()
        }
    }
}
