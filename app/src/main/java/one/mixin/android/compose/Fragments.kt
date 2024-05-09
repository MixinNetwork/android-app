package one.mixin.android.compose

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.Fragment
import one.mixin.android.R
import one.mixin.android.extension.findFragmentActivityOrNull
import one.mixin.android.extension.inTransaction
import one.mixin.android.ui.setting.LocalSettingNav
import timber.log.Timber

@Composable
fun MixinSettingFragment(
    tag: String? = null,
    createFragment: () -> Fragment,
) {
    val context = LocalContext.current

    val fragment =
        remember {
            createFragment()
        }

    val navigationController = LocalSettingNav.current

    val initialBackStackEntryCount =
        remember {
            val activity = context.findFragmentActivityOrNull()
            activity?.supportFragmentManager?.backStackEntryCount ?: 0
        }

    BackHandler {
        val activity = context.findFragmentActivityOrNull()
        val fragmentManager = activity?.supportFragmentManager
        fragmentManager?.popBackStack()
        Timber.d("back pressed: ${fragmentManager?.backStackEntryCount} $initialBackStackEntryCount")
        if (fragmentManager?.backStackEntryCount == initialBackStackEntryCount + 1) {
            navigationController.pop()
        }
    }

    DisposableEffect(fragment) {
        val activity = context.findFragmentActivityOrNull()
        val fragmentManager = activity?.supportFragmentManager

        fragmentManager?.inTransaction {
            setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_right,
                R.anim.slide_in_left,
                R.anim.slide_out_right,
            )
            add(R.id.container, fragment, tag)
            addToBackStack(null)
        }

        onDispose {
            fragmentManager?.popBackStack()
        }
    }

    // Add this as placeholder to ensure router animation work correctly.
    SettingPageScaffold(title = "") {
    }
}
