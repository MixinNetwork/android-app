package one.mixin.android.ui.setting

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.NavDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.toUri
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.UserBottomSheetDialogFragment
import one.mixin.android.ui.setting.ui.compose.MixinSettingFragment
import one.mixin.android.ui.setting.ui.page.*
import one.mixin.android.ui.setting.ui.theme.MixinAppTheme
import one.mixin.android.vo.User
import timber.log.Timber


enum class SettingDestination {
    Setting,
    Account,
    NotificationAndConfirm,
    BackUp,
    Storage,
    Appearance,
    Desktop,
    About,
    DatabaseDebug,
    AccountPrivacy,
    AccountSecurity,
    Blocked,
    Conversation,
    PhoneNumber,
    MobileContact,
    AppAuthSetting,
    UserBottomSheet,
}

open class SettingNavigationController {
    open fun navigation(destination: SettingDestination) {
        Timber.e("setting navigation: ${destination.name}")
    }

    open fun userBottomSheet(user: User, conversationId: String? = null) {
        Timber.e("userBottomSheet: ${user.userId}")
    }

    open fun pop() {
        Timber.e("setting pop")
    }
}

private const val USER_KEY = "user_key"
private const val CONVERSATION_ID_KEY = "conversation_id_key"

private class SettingNavControllerImpl(
    private val navController: NavController,
    private val closeActivity: () -> Unit
) : SettingNavigationController() {
    override fun navigation(destination: SettingDestination) {
        navigateTo(destination)
    }

    private fun navigateTo(dest: SettingDestination, args: Bundle? = null) {
        val routeLink = NavDeepLinkRequest
            .Builder
            .fromUri(NavDestination.createRoute(dest.name).toUri())
            .build()
        val deepLinkMatch = navController.graph.matchDeepLink(routeLink)
        if (deepLinkMatch == null) {
            Timber.e("navigateTo: no match for $dest")
            return
        }
        val id = deepLinkMatch.destination.id
        navController.navigate(id, args)
    }

    override fun userBottomSheet(user: User, conversationId: String?) {
        navigateTo(SettingDestination.UserBottomSheet, Bundle().apply {
            putParcelable(USER_KEY, user)
            putString(CONVERSATION_ID_KEY, conversationId)
        })
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
                MixinAppTheme(
                    darkTheme = context.isNightMode() || isSystemInDarkTheme(),
                ) {
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
                                composable(SettingDestination.Appearance.name) {
                                    AppearancePage()
                                }
                                composable(SettingDestination.NotificationAndConfirm.name) {
                                    NotificationsPage()
                                }
                                composable(SettingDestination.BackUp.name) {
                                    MixinSettingFragment(BackUpFragment.TAG) {
                                        BackUpFragment.newInstance()
                                    }
                                }
                                composable(SettingDestination.AccountPrivacy.name) {
                                    AccountPrivacyPage()
                                }
                                composable(SettingDestination.Blocked.name) {
                                    BlockedPage()
                                }

                                composable(SettingDestination.Conversation.name) {
                                    ConversationSettingPage()
                                }

                                composable(SettingDestination.PhoneNumber.name) {
                                    PhoneNumberSettingPage()
                                }

                                // TODO(BIN) remove this. didn't work now.
                                composable(SettingDestination.UserBottomSheet.name) {
                                    val user = it.arguments?.getParcelable<User>(USER_KEY)
                                    val conversationId = it.arguments?.getString(CONVERSATION_ID_KEY)

                                    val fragment = remember {
                                        if (user == null) {
                                            null
                                        } else {
                                            UserBottomSheetDialogFragment.newInstance(user, conversationId)
                                        }
                                    }
                                    if (fragment != null) {
                                        MixinSettingFragment(UserBottomSheetDialogFragment.TAG) {
                                            fragment
                                        }
                                    } else {
                                        LaunchedEffect(Unit) {
                                            navigationController.pop()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}