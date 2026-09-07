package one.mixin.android.ui.search

import kotlin.test.Test
import kotlin.test.assertEquals

class SearchFragmentTest {
    @Test
    fun searchDebounceIsHalfSecond() {
        assertEquals(500L, SearchFragment.SEARCH_DEBOUNCE)
    }
}
