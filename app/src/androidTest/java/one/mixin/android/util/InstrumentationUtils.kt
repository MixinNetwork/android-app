package one.mixin.android.util

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.test.platform.app.InstrumentationRegistry

fun isKeyboardShown(): Boolean {
    val inputMethodManager = InstrumentationRegistry.getInstrumentation().targetContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    return inputMethodManager.isAcceptingText
}
