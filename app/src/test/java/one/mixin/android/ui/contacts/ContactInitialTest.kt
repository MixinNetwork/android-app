package one.mixin.android.ui.contacts

import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class ContactInitialTest {
    @Test
    fun indexDragMapsPositionsAndClampsOutsideTheRail() {
        assertEquals("A", contactLetterAt(-20f, 378))
        assertEquals("A", contactLetterAt(13f, 378))
        assertEquals("B", contactLetterAt(14f, 378))
        assertEquals("N", contactLetterAt(189f, 378))
        assertEquals("#", contactLetterAt(378f, 378))
        assertEquals("#", contactLetterAt(500f, 378))
    }

    @Test
    fun initialsSupportLatinPinyinAndUnnamedContacts() {
        assertEquals("A", contactInitial(" alice "))
        assertEquals("E", contactInitial("Élodie"))
        assertEquals("B", contactInitial("白话区块链"))
        assertEquals("#", contactInitial(null))
        assertEquals("#", contactInitial("123"))
    }
}
