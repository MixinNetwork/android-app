package one.mixin.android.widget

import android.app.Activity
import android.content.Context
import android.os.Looper
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import one.mixin.android.MixinApplication
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.LooperMode
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
class CaptchaLoadTimeoutTest {
    @Test
    fun loadingProgressDoesNotExtendHCaptchaDeadline() =
        withCaptcha {
            startLoading(CaptchaView.CaptchaType.HCaptcha)
            val deadline = shadowOf(Looper.getMainLooper()).nextScheduledTaskTime
            assertEquals(35_000L, deadline.toMillis() - SystemClock.uptimeMillis())
            idle(2)
            progress(CaptchaLoadEvent.SdkLoaded, "sdk_loaded")
            idle(2)
            progress(CaptchaLoadEvent.WidgetRendered, "widget_rendered")

            assertEquals(deadline, shadowOf(Looper.getMainLooper()).nextScheduledTaskTime)
        }

    @Test
    fun answeringHasNoDeadlineEvenAfterLateProgress() =
        withCaptcha {
            startLoading(CaptchaView.CaptchaType.HCaptcha)
            progress(CaptchaLoadEvent.ChallengeReady, "challenge_ready")
            idle(1)
            progress(CaptchaLoadEvent.WidgetRendered, "widget_rendered")
            val state = CaptchaView::class.java.getDeclaredField("activeCaptchaLoad").apply { isAccessible = true }.get(this)
            assertNull(state.javaClass.getDeclaredField("timeoutRunnable").apply { isAccessible = true }.get(state))

            idle(60)

            assertEquals(CaptchaView.CaptchaType.HCaptcha, activeType())
        }

    @Test
    fun serverChallengesRetainAttemptHistoryUntilDismissed() =
        withCaptcha {
            val activity = Robolectric.buildActivity(Activity::class.java).setup()
            try {
                activity.get().setContentView(webView)
                startLoading(CaptchaView.CaptchaType.GTCaptcha)
                postToken("1", "test-response")
                shadowOf(Looper.getMainLooper()).idle()
                assertNull(CaptchaView::class.java.getDeclaredField("activeCaptchaLoad").apply { isAccessible = true }.get(this))

                startLoading(CaptchaView.CaptchaType.HCaptcha)
                assertEquals(listOf(CaptchaView.CaptchaType.GTCaptcha, CaptchaView.CaptchaType.HCaptcha), attemptedTypes())

                startLoading(CaptchaView.CaptchaType.GTCaptcha)
                assertEquals(listOf(CaptchaView.CaptchaType.HCaptcha, CaptchaView.CaptchaType.GTCaptcha), attemptedTypes())

                hide()
                assertEquals(emptyList<CaptchaView.CaptchaType>(), attemptedTypes())
            } finally {
                activity.pause().stop().destroy()
            }
        }

    private fun withCaptcha(block: CaptchaView.() -> Unit) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        MixinApplication.appContext = context
        val captcha =
            CaptchaView(
                context,
                object : CaptchaView.Callback {
                    override fun onStop() = Unit

                    override fun onPostToken(value: Pair<CaptchaView.CaptchaType, String>) = Unit
                },
            )
        try {
            captcha.block()
        } finally {
            captcha.release()
        }
    }

    private fun idle(seconds: Long) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(seconds))
    }

    private fun CaptchaView.startLoading(type: CaptchaView.CaptchaType) {
        CaptchaView::class.java
            .getDeclaredMethod("startCaptchaLoad", CaptchaView.CaptchaType::class.java, Boolean::class.javaPrimitiveType)
            .apply { isAccessible = true }
            .invoke(this, type, true)
    }

    private fun CaptchaView.progress(
        event: CaptchaLoadEvent,
        stage: String,
    ) {
        CaptchaView::class.java
            .getDeclaredMethod("handleCaptchaLoadEvent", String::class.java, CaptchaLoadEvent::class.java, String::class.java)
            .apply { isAccessible = true }
            .invoke(this, "1", event, stage)
    }

    private fun CaptchaView.activeType(): CaptchaView.CaptchaType {
        val state = CaptchaView::class.java.getDeclaredField("activeCaptchaLoad").apply { isAccessible = true }.get(this)
        return state.javaClass.getDeclaredField("type").apply { isAccessible = true }.get(state) as CaptchaView.CaptchaType
    }

    private fun CaptchaView.attemptedTypes(): List<*> =
        CaptchaView::class.java.getDeclaredField("attemptedCaptchaTypes").apply { isAccessible = true }.get(this) as List<*>
}
