package one.mixin.android.widget

import android.content.Context
import android.os.Looper
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.test.core.app.ApplicationProvider
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import one.mixin.android.R
import one.mixin.android.ui.search.SearchFragment.Companion.SEARCH_DEBOUNCE
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class MaterialSearchViewTest {
    private fun createSearchView(): MaterialSearchView {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        val themedContext = ContextThemeWrapper(baseContext, R.style.AppTheme_NoActionBar)
        return MaterialSearchView(themedContext)
    }

    private fun advanceMainLooper(millis: Long) {
        shadowOf(Looper.getMainLooper()).idleFor(millis, TimeUnit.MILLISECONDS)
    }

    @Test
    fun closeSearchDuringOpenAnimationRestoresLogoAndHidesSearchInputs() {
        val searchView = createSearchView()
        val logoLayout = searchView.findViewById<View>(R.id.logo_layout)
        val searchEt = searchView.findViewById<EditText>(R.id.search_et)
        val backIb = searchView.findViewById<ImageButton>(R.id.back_ib)
        val rightClear = searchView.findViewById<ImageButton>(R.id.right_clear)

        searchView.openSearch()
        advanceMainLooper(50L)
        searchView.closeSearch()
        advanceMainLooper(500L)

        assertFalse(searchView.isOpen)
        assertTrue(logoLayout.isVisible)
        assertEquals(1f, logoLayout.alpha)
        assertTrue(searchEt.isGone)
        assertEquals(0f, searchEt.alpha)
        assertTrue(backIb.isGone)
        assertEquals(0f, backIb.alpha)
        assertTrue(rightClear.isGone)
    }

    @Test
    fun dragSearchPartialProgressThenCloseRestoresLogoAndHidesInputs() {
        val searchView = createSearchView()
        val logoLayout = searchView.findViewById<View>(R.id.logo_layout)
        val searchEt = searchView.findViewById<EditText>(R.id.search_et)
        val backIb = searchView.findViewById<ImageButton>(R.id.back_ib)
        val rightClear = searchView.findViewById<ImageButton>(R.id.right_clear)

        searchView.dragSearch(0.2f)
        assertTrue(logoLayout.isVisible)
        assertFalse(searchEt.isVisible)
        assertFalse(backIb.isVisible)

        searchView.closeSearch()
        advanceMainLooper(500L)

        assertFalse(searchView.isOpen)
        assertTrue(logoLayout.isVisible)
        assertEquals(1f, logoLayout.alpha)
        assertTrue(searchEt.isGone)
        assertTrue(backIb.isGone)
        assertTrue(rightClear.isGone)
    }

    @Test
    fun focusSearchPreservesActiveQueryAndClearButton() {
        val searchView = createSearchView()
        val searchEt = searchView.findViewById<EditText>(R.id.search_et)
        val rightClear = searchView.findViewById<ImageButton>(R.id.right_clear)

        searchView.openSearch()
        advanceMainLooper(500L)
        searchEt.setText("btc")
        Thread.sleep(SEARCH_DEBOUNCE + 150L)
        advanceMainLooper(50L)
        assertTrue(rightClear.isVisible)
        searchEt.setSelection(0)
        searchEt.clearFocus()

        searchView.focusSearch()
        advanceMainLooper(500L)

        assertTrue(searchView.isOpen)
        assertEquals("btc", searchEt.text.toString())
        assertEquals(3, searchEt.selectionStart)
        assertTrue(rightClear.isVisible)
        assertTrue(searchEt.hasFocus())
    }

    @Test
    fun closeSearchHidesRightClearAndIgnoresLateTextChanges() {
        val searchView = createSearchView()
        val searchEt = searchView.findViewById<EditText>(R.id.search_et)
        val rightClear = searchView.findViewById<ImageButton>(R.id.right_clear)
        val queries = mutableListOf<String>()

        searchView.mOnQueryTextListener =
            object : MaterialSearchView.OnQueryTextListener {
                override fun onQueryTextChange(newText: String): Boolean {
                    queries.add(newText)
                    return true
                }
            }

        searchView.openSearch()
        advanceMainLooper(500L)

        searchEt.setText("btc")
        Thread.sleep(SEARCH_DEBOUNCE + 150L)
        advanceMainLooper(50L)
        assertTrue(rightClear.isVisible)

        queries.clear()
        searchEt.setText("eth")
        searchView.closeSearch()
        Thread.sleep(SEARCH_DEBOUNCE + 150L)
        advanceMainLooper(500L)

        assertFalse(searchView.isOpen)
        assertTrue(rightClear.isGone)
        assertEquals("", searchEt.text.toString())
        assertTrue(queries.none { it.isNotEmpty() })
    }
}
