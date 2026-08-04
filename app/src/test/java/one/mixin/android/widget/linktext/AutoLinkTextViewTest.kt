package one.mixin.android.widget.linktext

import android.content.Context
import androidx.emoji2.bundled.BundledEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat
import androidx.test.core.app.ApplicationProvider
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AutoLinkTextViewTest {
    @BeforeTest
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        runCatching { EmojiCompat.get() }
            .getOrElse { EmojiCompat.init(BundledEmojiCompatConfig(context)) }
    }

    @Test
    fun setTextWithoutAutoLinkModeRendersPlainText() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val textView = AutoLinkTextView(context, null)

        textView.text = "Ethereum is a decentralized open-source blockchain."

        assertEquals("Ethereum is a decentralized open-source blockchain.", textView.text.toString())
    }
}
