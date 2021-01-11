package one.mixin.android.extension

import android.content.ComponentName
import android.content.Intent
import androidx.annotation.StyleRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import one.mixin.android.HiltTestActivity

inline fun <reified T : Fragment> launchFragmentInHiltContainer(
    fragment: T,
    @StyleRes themeResId: Int,
    crossinline action: T.() -> Unit = {}
) {
    val startActivityIntent = Intent.makeMainActivity(
        ComponentName(
            ApplicationProvider.getApplicationContext(),
            HiltTestActivity::class.java
        )
    ).putExtra("androidx.fragment.app.testing.FragmentScenario.EmptyFragmentActivity.THEME_EXTRAS_BUNDLE_KEY", themeResId)

    ActivityScenario.launch<HiltTestActivity>(startActivityIntent).onActivity { activity ->
        if (fragment is DialogFragment) {
            fragment.showNow(activity.supportFragmentManager, "")
        } else {
            activity.supportFragmentManager
                .beginTransaction()
                .add(android.R.id.content, fragment, "")
                .commitNow()
        }
        fragment.action()
    }
}

inline fun <reified T : Fragment> launchFragmentInHiltContainer(
    @StyleRes themeResId: Int,
    crossinline initFragmentAction: () -> T,
    crossinline action: T.() -> Unit = {}
) {
    val startActivityIntent = Intent.makeMainActivity(
        ComponentName(
            ApplicationProvider.getApplicationContext(),
            HiltTestActivity::class.java
        )
    ).putExtra("androidx.fragment.app.testing.FragmentScenario.EmptyFragmentActivity.THEME_EXTRAS_BUNDLE_KEY", themeResId)

    ActivityScenario.launch<HiltTestActivity>(startActivityIntent).onActivity { activity ->
        val fragment = initFragmentAction.invoke()
        if (fragment is DialogFragment) {
            fragment.showNow(activity.supportFragmentManager, "")
        } else {
            activity.supportFragmentManager
                .beginTransaction()
                .add(android.R.id.content, fragment, "")
                .commitNow()
        }
        fragment.action()
    }
}
