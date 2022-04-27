package one.mixin.android.ui.setting

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.setting.ui.page.AboutPage
import one.mixin.android.ui.setting.ui.page.AccountPage
import one.mixin.android.ui.setting.ui.page.SettingPage
import one.mixin.android.ui.setting.ui.theme.MixinAppTheme
import timber.log.Timber


enum class SettingDestination {
    Setting,
    Account,
    NotificationAndConfirm,
    Backup,
    Storage,
    Appearance,
    Desktop,
    About,
    DatabaseDebug,
    AccountPrivacy,
    AccountSecurity,
}

open class SettingNavigationController {
    open fun navigation(destination: SettingDestination) {
        Timber.e("setting navigation: ${destination.name}")
    }

    open fun pop() {
        Timber.e("setting pop")
    }
}

private class SettingNavControllerImpl(
    private val navController: NavController,
    private val closeActivity: () -> Unit
) : SettingNavigationController() {
    override fun navigation(destination: SettingDestination) {
        navController.navigate(destination.name)
    }

    override fun pop() {
        if (!navController.popBackStack()) {
            closeActivity()
        }
    }
}

val LocalSettingNav =
    compositionLocalOf { SettingNavigationController() }

class SettingComposeFragment : BaseFragment() {

    companion object {

        const val TAG = "SettingComposeFragment"

        fun newInstance(): SettingComposeFragment {
            return SettingComposeFragment()
        }

    }

    private val parentBackStackEntryCount = MutableLiveData(0)

    private val onParentBackStackChanged = FragmentManager.OnBackStackChangedListener {
        parentBackStackEntryCount.value = parentFragmentManager.backStackEntryCount
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        parentFragmentManager.addOnBackStackChangedListener(onParentBackStackChanged)
    }

    override fun onDetach() {
        super.onDetach()
        parentFragmentManager.removeOnBackStackChangedListener(onParentBackStackChanged)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(inflater.context).apply {
            setContent {
                MixinAppTheme {
                    LocalOnBackPressedDispatcherOwner
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colors.background
                    ) {
                        val navController = rememberNavController()
                        val navigationController = remember {
                            SettingNavControllerImpl(navController, closeActivity = {
                                activity?.onBackPressed()
                            })
                        }

                        DisposableEffect(navController) {
                            val observer = Observer<Int> { value ->
                                navController.enableOnBackPressed(value == 0)
                            }
                            parentBackStackEntryCount.observeForever(observer)
                            onDispose {
                                parentBackStackEntryCount.removeObserver(observer)
                            }
                        }

                        CompositionLocalProvider(
                            LocalSettingNav provides navigationController
                        ) {
                            NavHost(
                                navController = navController,
                                startDestination = SettingDestination.Setting.name,
                            ) {
                                composable(SettingDestination.Setting.name) {
                                    SettingPage()
                                }
                                composable(SettingDestination.About.name) {
                                    AboutPage()
                                }
                                composable(SettingDestination.Account.name) {
                                    AccountPage()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}