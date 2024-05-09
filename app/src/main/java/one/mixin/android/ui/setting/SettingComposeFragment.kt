package one.mixin.android.ui.setting

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.FragmentManager
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.NavDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import one.mixin.android.api.response.AuthorizationResponse
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.toUri
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.UserBottomSheetDialogFragment
import one.mixin.android.ui.setting.delete.DeleteAccountFragment
import one.mixin.android.compose.MixinSettingFragment
import one.mixin.android.ui.setting.ui.page.AboutPage
import one.mixin.android.ui.setting.ui.page.AccountPage
import one.mixin.android.ui.setting.ui.page.AccountPrivacyPage
import one.mixin.android.ui.setting.ui.page.AppAuthSettingPage
import one.mixin.android.ui.setting.ui.page.AppearancePage
import one.mixin.android.ui.setting.ui.page.AuthenticationsPage
import one.mixin.android.ui.setting.ui.page.AuthenticationsViewModel
import one.mixin.android.ui.setting.ui.page.BiometricTimePage
import one.mixin.android.ui.setting.ui.page.BlockedPage
import one.mixin.android.ui.setting.ui.page.ConversationSettingPage
import one.mixin.android.ui.setting.ui.page.EmergencyContactPage
import one.mixin.android.ui.setting.ui.page.MobileContactPage
import one.mixin.android.ui.setting.ui.page.NotificationsPage
import one.mixin.android.ui.setting.ui.page.PermissionListPage
import one.mixin.android.ui.setting.ui.page.PhoneNumberSettingPage
import one.mixin.android.ui.setting.ui.page.PinLogsPage
import one.mixin.android.ui.setting.ui.page.PinSettingPage
import one.mixin.android.ui.setting.ui.page.SecurityPage
import one.mixin.android.ui.setting.ui.page.SettingPage
import one.mixin.android.ui.setting.ui.page.ViewEmergencyContactPage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.vo.User
import timber.log.Timber

enum class SettingDestination {
    Setting,
    Account,
    NotificationAndConfirm,
    MigrateRestore,
    DataStorage,
    Appearance,
    About,
    DatabaseDebug,
    AccountPrivacy,
    AccountSecurity,
    DeleteAccount,
    Blocked,
    Conversation,
    PhoneNumber,
    MobileContact,
    AppAuthSetting,
    UserBottomSheet,
    PinSetting,
    EmergencyContact,
    ViewEmergencyContact,
    Authentications,
    AuthenticationPermissions,
    PinLogs,
    BiometricTime,
    Wallpaper,
    LogAndDebug,
}

open class SettingNavigationController {
    open fun navigation(
        destination: SettingDestination,
        args: Bundle? = null,
    ) {
        Timber.e("setting navigation: ${destination.name}")
    }

    open fun userBottomSheet(
        user: User,
        conversationId: String? = null,
    ) {
        Timber.e("userBottomSheet: ${user.userId}")
    }

    open fun viewEmergencyContact(user: User) {
        Timber.e("viewEmergencyContact: ${user.userId}")
    }

    open fun authorizationPermissions(auth: AuthorizationResponse) {
        Timber.e("authorizationPermissions: ${auth.authorizationId}")
    }

    open fun pop() {
        Timber.e("setting pop")
    }
}

private const val USER_KEY = "user_key"
private const val CONVERSATION_ID_KEY = "conversation_id_key"
private const val AUTHORIZATION_KEY = "authorization_key"

private class SettingNavControllerImpl(
    private val navController: NavController,
    private val closeActivity: () -> Unit,
) : SettingNavigationController() {
    override fun navigation(
        destination: SettingDestination,
        args: Bundle?,
    ) {
        navigateTo(destination, args)
    }

    private fun navigateTo(
        dest: SettingDestination,
        args: Bundle? = null,
    ) {
        val routeLink =
            NavDeepLinkRequest
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

    override fun userBottomSheet(
        user: User,
        conversationId: String?,
    ) {
        navigateTo(
            SettingDestination.UserBottomSheet,
            Bundle().apply {
                putParcelable(USER_KEY, user)
                putString(CONVERSATION_ID_KEY, conversationId)
            },
        )
    }

    override fun pop() {
        if (!navController.popBackStack()) {
            closeActivity()
        }
    }

    override fun viewEmergencyContact(user: User) {
        navigateTo(
            SettingDestination.ViewEmergencyContact,
            Bundle().apply {
                putParcelable(USER_KEY, user)
            },
        )
    }

    override fun authorizationPermissions(auth: AuthorizationResponse) {
        navigateTo(
            SettingDestination.AuthenticationPermissions,
            Bundle().apply {
                putParcelable(AUTHORIZATION_KEY, auth)
            },
        )
    }
}

val LocalSettingNav =
    compositionLocalOf { SettingNavigationController() }

@Suppress("OPT_IN_IS_NOT_ENABLED")
@OptIn(ExperimentalAnimationApi::class)
class SettingComposeFragment : BaseFragment() {
    companion object {
        const val TAG = "SettingComposeFragment"

        fun newInstance(): SettingComposeFragment {
            return SettingComposeFragment()
        }
    }

    private val parentBackStackEntryCount = MutableLiveData(0)

    private val onParentBackStackChanged =
        FragmentManager.OnBackStackChangedListener {
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(inflater.context).apply {
            setContent {
                MixinAppTheme(
                    darkTheme = context.isNightMode(),
                ) {
                    val navController = rememberNavController()
                    val navigationController =
                        remember {
                            SettingNavControllerImpl(navController, closeActivity = {
                                activity?.onBackPressedDispatcher?.onBackPressed()
                            })
                        }

                    DisposableEffect(navController) {
                        val observer =
                            Observer<Int> { value ->
                                navController.enableOnBackPressed(value == 0)
                            }
                        parentBackStackEntryCount.observeForever(observer)
                        onDispose {
                            parentBackStackEntryCount.removeObserver(observer)
                        }
                    }

                    CompositionLocalProvider(
                        LocalSettingNav provides navigationController,
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = SettingDestination.Setting.name,
                            enterTransition = {
                                slideIntoContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Left,
                                    animationSpec = tween(300),
                                )
                            },
                            popEnterTransition = {
                                slideIntoContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Right,
                                    animationSpec = tween(300),
                                )
                            },
                            exitTransition = {
                                slideOutOfContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Left,
                                    animationSpec = tween(300),
                                )
                            },
                            popExitTransition = {
                                slideOutOfContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Right,
                                    animationSpec = tween(300),
                                )
                            },
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
                            composable(SettingDestination.DataStorage.name) {
                                MixinSettingFragment(SettingDataStorageFragment.TAG) {
                                    SettingDataStorageFragment.newInstance()
                                }
                            }
                            composable(SettingDestination.MigrateRestore.name) {
                                MixinSettingFragment(MigrateRestoreFragment.TAG) {
                                    MigrateRestoreFragment.newInstance()
                                }
                            }
                            composable(SettingDestination.LogAndDebug.name) {
                                MixinSettingFragment(LogAndDebugFragment.TAG) {
                                    LogAndDebugFragment.newInstance()
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

                            composable(SettingDestination.MobileContact.name) {
                                MobileContactPage()
                            }

                            composable(SettingDestination.AppAuthSetting.name) {
                                AppAuthSettingPage()
                            }

                            composable(SettingDestination.AccountSecurity.name) {
                                SecurityPage()
                            }

                            composable(SettingDestination.PinSetting.name) {
                                PinSettingPage()
                            }

                            composable(SettingDestination.BiometricTime.name) {
                                BiometricTimePage()
                            }

                            composable(SettingDestination.EmergencyContact.name) {
                                EmergencyContactPage()
                            }

                            composable(SettingDestination.ViewEmergencyContact.name) {
                                val user = it.arguments?.getParcelableCompat(USER_KEY, User::class.java)
                                if (user == null) {
                                    Timber.e("viewEmergencyContact: no user")
                                    return@composable
                                }
                                ViewEmergencyContactPage(user)
                            }

                            composable(SettingDestination.Authentications.name) {
                                AuthenticationsPage()
                            }

                            composable(SettingDestination.AuthenticationPermissions.name) { backStackEntry ->
                                val auth =
                                    backStackEntry.arguments?.getParcelableCompat(
                                        AUTHORIZATION_KEY,
                                        AuthorizationResponse::class.java,
                                    )
                                if (auth == null) {
                                    Timber.e("viewEmergencyContact: no auth")
                                    return@composable
                                }
                                val parentEntry =
                                    remember(backStackEntry) {
                                        try {
                                            navController.getBackStackEntry(SettingDestination.Authentications.name)
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }
                                val authViewModel =
                                    parentEntry?.let { hiltViewModel<AuthenticationsViewModel>(it) }
                                PermissionListPage(auth, authViewModel)
                            }

                            composable(SettingDestination.PinLogs.name) {
                                PinLogsPage()
                            }

                            composable(SettingDestination.UserBottomSheet.name) {
                                val user = it.arguments?.getParcelableCompat(USER_KEY, User::class.java)
                                val conversationId = it.arguments?.getString(CONVERSATION_ID_KEY)

                                val fragment =
                                    remember {
                                        if (user == null) {
                                            null
                                        } else {
                                            UserBottomSheetDialogFragment.newInstance(
                                                user,
                                                conversationId,
                                            )
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

                            composable(SettingDestination.DeleteAccount.name) {
                                MixinSettingFragment(DeleteAccountFragment.TAG) {
                                    DeleteAccountFragment.newInstance()
                                }
                            }

                            composable(SettingDestination.DatabaseDebug.name) {
                                MixinSettingFragment(DatabaseDebugFragment.TAG) {
                                    DatabaseDebugFragment.newInstance()
                                }
                            }

                            composable(SettingDestination.Wallpaper.name) {
                                MixinSettingFragment(SettingWallpaperFragment.TAG) {
                                    SettingWallpaperFragment.newInstance()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
