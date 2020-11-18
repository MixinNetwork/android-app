package one.mixin.android.extension

import android.app.Activity
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Description
import org.hamcrest.Matcher

fun ViewInteraction.isDisplayed(): Boolean {
    return try {
        check(matches(ViewMatchers.isDisplayed()))
        true
    } catch (e: NoMatchingViewException) {
        false
    }
}

fun withTextColor(expectedId: Int): Matcher<View?>? {
    return object : BoundedMatcher<View?, TextView>(TextView::class.java) {
        override fun matchesSafely(textView: TextView): Boolean {
            val colorId = ContextCompat.getColor(textView.context, expectedId)
            return textView.currentTextColor == colorId
        }

        override fun describeTo(description: Description) {
            description.appendText("with text color: ")
            description.appendValue(expectedId)
        }
    }
}

fun <T : Activity> ActivityScenario<T>.getRVCount(recyclerViewId: Int): Int {
    var count = 0
    onActivity { activity ->
        val rv = activity.findViewById<RecyclerView>(recyclerViewId)
        count = rv?.adapter?.itemCount ?: 0
    }
    return count
}
