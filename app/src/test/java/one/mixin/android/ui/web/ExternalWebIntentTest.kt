package one.mixin.android.ui.web

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class ExternalWebIntentTest {
    @Test
    fun stripsExplicitTargetsAndUriFlagsButKeepsExternalPackage() {
        val parsed = parse(packagedAttack())
        assertEquals(ATTACK_COMPONENT, parsed.component)
        assertEquals(Intent.ACTION_SEND, parsed.action)
        assertTrue((parsed.flags and Intent.FLAG_ACTIVITY_CLEAR_TASK) != 0)

        var componentAtResolve: ComponentName? = ATTACK_COMPONENT
        var selectorAtResolve: Intent? = Intent()
        val allowed =
            parsed.prepareForExternalWebLaunch(
                ownPackage = OWN_PACKAGE,
                requireNonBrowser = true,
                newTask = true,
            ) {
                componentAtResolve = parsed.component
                selectorAtResolve = parsed.selector
                OTHER_PACKAGE
            }

        assertTrue(allowed)
        assertNull(componentAtResolve)
        assertNull(selectorAtResolve)
        assertNull(parsed.component)
        assertNull(parsed.selector)
        assertEquals(Intent.ACTION_VIEW, parsed.action)
        assertTrue(parsed.hasCategory(Intent.CATEGORY_BROWSABLE))
        assertTrue(parsed.hasCategory(Intent.CATEGORY_DEFAULT))
        assertEquals(OTHER_PACKAGE, parsed.`package`)
        assertEquals(Uri.parse("mixin://pay/1"), parsed.data)
        assertEquals(FALLBACK, parsed.getStringExtra("browser_fallback_url"))
        assertEquals(
            Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER or Intent.FLAG_ACTIVITY_NEW_TASK,
            parsed.flags,
        )
    }

    @Test
    fun stripsSelectorUsedToTargetAnInternalActivity() {
        val parsed = parse(selectorAttack())
        assertNull(parsed.`package`)
        assertEquals(ATTACK_COMPONENT, parsed.component)
        assertEquals(SELECTOR_COMPONENT, parsed.selector?.component)

        val allowed =
            parsed.prepareForExternalWebLaunch(
                ownPackage = OWN_PACKAGE,
                requireNonBrowser = false,
                newTask = false,
            ) {
                assertNull(parsed.selector)
                null
            }

        assertTrue(allowed)
        assertNull(parsed.component)
        assertNull(parsed.selector)
        assertEquals(0, parsed.flags)
        assertEquals(Intent.ACTION_VIEW, parsed.action)
        assertTrue(parsed.hasCategory(Intent.CATEGORY_BROWSABLE))
    }

    @Test
    fun refusesResolvedOwnPackageAndAllowsOtherOrUnresolvedTargets() {
        val refused = parse(packagedAttack())
        assertFalse(launchParsed(refused, OWN_PACKAGE))
        assertNull(refused.component)
        assertNull(refused.selector)
        assertEquals(OTHER_PACKAGE, refused.`package`)
        assertEquals(Intent.ACTION_VIEW, refused.action)
        assertTrue(launch(packagedAttack(), OTHER_PACKAGE))
        assertTrue(launch(packagedAttack(), null))
    }

    @Test
    fun contextOverloadDropsUriFlagsBeforeResolving() {
        val parsed = parse(packagedAttack())
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertTrue(parsed.prepareForExternalWebLaunch(context))
        assertNull(parsed.component)
        assertEquals(OTHER_PACKAGE, parsed.`package`)
        val allowedFlags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER
        assertEquals(0, parsed.flags and allowedFlags.inv())
    }

    private fun launch(
        intent: Intent,
        resolvedPackage: String?,
    ): Boolean = launchParsed(parse(intent), resolvedPackage)

    private fun launchParsed(
        intent: Intent,
        resolvedPackage: String?,
    ): Boolean =
        intent.prepareForExternalWebLaunch(
            ownPackage = OWN_PACKAGE,
            requireNonBrowser = false,
            newTask = false,
        ) { resolvedPackage }

    private fun parse(intent: Intent): Intent =
        Intent.parseUri(intent.toUri(Intent.URI_INTENT_SCHEME), Intent.URI_INTENT_SCHEME)

    private fun packagedAttack(): Intent =
        Intent(Intent.ACTION_SEND).apply {
            component = ATTACK_COMPONENT
            setPackage(OTHER_PACKAGE)
            addCategory(Intent.CATEGORY_DEFAULT)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            data = Uri.parse("mixin://pay/1")
            putExtra("browser_fallback_url", FALLBACK)
        }

    private fun selectorAttack(): Intent =
        Intent(Intent.ACTION_SEND).apply {
            component = ATTACK_COMPONENT
            selector =
                Intent().apply {
                    component = SELECTOR_COMPONENT
                }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            data = Uri.parse("example://open")
        }

    private companion object {
        const val OWN_PACKAGE = "one.mixin.messenger"
        const val OTHER_PACKAGE = "com.example.other"
        const val FALLBACK = "https://example.com/fallback"
        val ATTACK_COMPONENT = ComponentName(OWN_PACKAGE, "one.mixin.android.ui.landing.InitializeActivity")
        val SELECTOR_COMPONENT = ComponentName(OWN_PACKAGE, "one.mixin.android.ui.landing.RestoreActivity")
    }
}
