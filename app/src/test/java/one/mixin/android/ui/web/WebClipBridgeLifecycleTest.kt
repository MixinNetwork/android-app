package one.mixin.android.ui.web

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.google.mlkit.common.internal.CommonComponentRegistrar
import com.google.mlkit.common.sdkinternal.MlKitContext
import com.google.mlkit.vision.barcode.internal.BarcodeRegistrar
import com.google.mlkit.vision.common.internal.VisionCommonRegistrar
import one.mixin.android.ui.qr.QRCodeProcessor
import one.mixin.android.vo.App
import one.mixin.android.widget.MixinWebView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, manifest = Config.NONE, sdk = [28])
class WebClipBridgeLifecycleTest {
    @Test
    fun retainedMixinContextDoesNotCallPreviousOwnerAfterCreatingWebClip() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        MlKitContext.initializeIfNeeded(
            context,
            listOf(CommonComponentRegistrar(), VisionCommonRegistrar(), BarcodeRegistrar()),
        )
        val activityController = Robolectric.buildActivity(Activity::class.java).setup()
        val activity = activityController.get()
        val webView = MixinWebView(activity)
        // ShadowView.onLayout replaces FrameLayout's child layout; set only the backing bounds
        // that the real generateWebClip reads before creating and drawing its bitmap.
        ReflectionHelpers.setField(webView, "mLeft", 0)
        ReflectionHelpers.setField(webView, "mTop", 0)
        ReflectionHelpers.setField(webView, "mRight", 100)
        ReflectionHelpers.setField(webView, "mBottom", 100)
        val fragment = WebFragment.newInstance(Bundle().apply { putString(WebFragment.URL, "https://example.com") })
        var oldOwnerCalls = 0
        val retainedBridge =
            WebFragment.WebAppInterface(
                context,
                conversationId = null,
                immersive = false,
                closeAction = { oldOwnerCalls++ },
            )

        try {
            assertEquals("Fixture WebView must have a width", 100, webView.width)
            assertEquals("Fixture WebView must have a height", 100, webView.height)
            webView.addJavascriptInterface(retainedBridge, "MixinContext")
            setField(fragment, "webView", webView)
            setField(fragment, "webAppInterface", retainedBridge)
            setField(fragment, "app", botApp)

            retainedBridge.close()
            assertEquals("Attached owner must receive the call", 1, oldOwnerCalls)

            val clip =
                WebFragment::class.java.getDeclaredMethod("generateWebClip").apply {
                    isAccessible = true
                }.invoke(fragment) as WebClip?

            assertNotNull("Exercise the actual detach path, not the zero-height early return", clip)
            assertSame("The floating clip must preserve its WebView", webView, clip!!.webView)
            retainedBridge.close()
            assertEquals("A retained MixinContext must not call the detached owner", 1, oldOwnerCalls)
        } finally {
            val processor =
                WebFragment::class.java.getDeclaredField("processor").apply {
                    isAccessible = true
                }.get(fragment) as QRCodeProcessor
            processor.close()
            webView.destroy()
            activityController.pause().stop().destroy()
        }
    }

    private fun setField(fragment: WebFragment, name: String, value: Any) {
        WebFragment::class.java.getDeclaredField(name).apply {
            isAccessible = true
        }.set(fragment, value)
    }

    private companion object {
        val botApp =
            App(
                appId = "bridge-test",
                appNumber = "70001001000",
                homeUri = "https://example.com",
                redirectUri = "",
                name = "Bridge test",
                iconUrl = "",
                category = null,
                description = "",
                appSecret = "",
                capabilities = null,
                creatorId = "bridge-test",
                resourcePatterns = null,
                updatedAt = null,
            )
    }
}
