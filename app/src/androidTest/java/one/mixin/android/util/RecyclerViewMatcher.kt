package one.mixin.android.util

import android.content.res.Resources
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.action.GeneralSwipeAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Swipe
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

class RecyclerViewMatcher(private val recyclerViewId: Int) {

    fun atPosition(position: Int): Matcher<View> {
        return atPositionOnView(position, -1)
    }

    fun atPositionOnView(position: Int, targetViewId: Int): Matcher<View> {
        return object : TypeSafeMatcher<View>() {
            var resources: Resources? = null
            var childView: View? = null

            override fun describeTo(description: Description?) {
                var idDescription: String = recyclerViewId.toString()
                if (resources != null) {
                    idDescription = try {
                        resources!!.getResourceName(recyclerViewId)
                    } catch (e: Resources.NotFoundException) {
                        "$recyclerViewId (resource name not found)"
                    }
                }
                description?.appendText("with id: $idDescription")
            }

            override fun matchesSafely(view: View): Boolean {
                this.resources = view.resources
                if (childView == null) {
                    val rv = view.rootView.findViewById<RecyclerView>(recyclerViewId)
                    if (rv != null && rv.id == recyclerViewId) {
                        childView = rv.findViewHolderForAdapterPosition(position)?.itemView
                    } else {
                        return false
                    }
                }
                return if (targetViewId == -1) {
                    view == childView
                } else {
                    val targetView = childView?.findViewById<View>(targetViewId)
                    view == targetView
                }
            }
        }
    }
}

fun withRecyclerView(recyclerViewId: Int) = RecyclerViewMatcher(recyclerViewId)

fun swipeRight(): GeneralSwipeAction {
    return GeneralSwipeAction(
        Swipe.FAST, GeneralLocation.CENTER_LEFT,
        GeneralLocation.CENTER_RIGHT, Press.FINGER
    )
}
