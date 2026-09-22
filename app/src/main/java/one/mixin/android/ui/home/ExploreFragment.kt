package one.mixin.android.ui.home

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.edit
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.databinding.FragmentExploreBinding
import one.mixin.android.event.BadgeEvent
import one.mixin.android.event.BotEvent
import one.mixin.android.event.FavoriteEvent
import one.mixin.android.event.SessionEvent
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.navTo
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.QrBottomSheetDialogFragment
import one.mixin.android.ui.common.profile.MySharedAppsFragment
import one.mixin.android.ui.common.profile.ProfileBottomSheetDialogFragment
import one.mixin.android.ui.common.showUserBottom
import one.mixin.android.ui.contacts.ContactListFragment
import one.mixin.android.ui.contacts.ContactViewModel
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.device.DeviceFragment
import one.mixin.android.ui.home.bot.Bot
import one.mixin.android.ui.home.bot.BotManagerViewModel
import one.mixin.android.ui.home.bot.INTERNAL_BUY_ID
import one.mixin.android.ui.home.bot.INTERNAL_LINK_DESKTOP_ID
import one.mixin.android.ui.home.bot.INTERNAL_MEMBER_ID
import one.mixin.android.ui.home.bot.INTERNAL_REFERRAL_ID
import one.mixin.android.ui.home.bot.INTERNAL_SUPPORT_ID
import one.mixin.android.ui.home.bot.INTERNAL_SWAP_ID
import one.mixin.android.ui.home.reminder.RecoveryReminderBottomSheetDialogFragment
import one.mixin.android.ui.home.web3.trade.SwapActivity
import one.mixin.android.ui.search.SearchExploreFragment
import one.mixin.android.ui.setting.SettingActivity
import one.mixin.android.ui.setting.member.MixinMemberInvoicesFragment
import one.mixin.android.ui.setting.member.MixinMemberUpgradeBottomSheetDialogFragment
import one.mixin.android.ui.url.UrlInterpreterActivity
import one.mixin.android.ui.wallet.WalletActivity
import one.mixin.android.ui.web.WebActivity
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.util.analytics.AnalyticsTracker.TradeWallet
import one.mixin.android.util.rxpermission.RxPermissions
import one.mixin.android.vo.BotInterface
import one.mixin.android.vo.ExploreApp
import one.mixin.android.vo.Plan
import one.mixin.android.vo.toUser

@AndroidEntryPoint
class ExploreFragment : BaseFragment() {
    companion object {
        const val TAG = "ExploreFragment"
        const val PREF_BOT_CLICKED_IDS = "explore_bot_clicked_ids"
        private const val ARG_BOTS = "bots"
        val SHOW_DOT_BOT_IDS = setOf(INTERNAL_BUY_ID, INTERNAL_SWAP_ID, INTERNAL_MEMBER_ID, INTERNAL_REFERRAL_ID)
        fun newInstance(bots: Boolean = false) = ExploreFragment().withArgs { putBoolean(ARG_BOTS, bots) }
    }

    private var _binding: FragmentExploreBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val botManagerViewModel by viewModels<BotManagerViewModel>()
    private val contactViewModel by viewModels<ContactViewModel>()
    private val contacts by lazy { contactViewModel.findContacts() }
    private val self by lazy { contactViewModel.findSelf() }
    private var favoriteApps by mutableStateOf<List<ExploreApp>>(emptyList())
    private var apps by mutableStateOf<List<ExploreApp>>(emptyList())
    private var isDesktopLogin by mutableStateOf(false)
    private var clickedBotIds by mutableStateOf<Set<String>>(emptySet())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentExploreBinding.inflate(inflater, container, false)
        val bots = arguments?.getBoolean(ARG_BOTS) == true
        binding.homeToolbar.isVisible = !bots
        binding.compose.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        binding.compose.setContent {
            if (bots) {
                BotsPage(
                    favorites = favoriteApps,
                    apps = apps,
                    onBack = { requireActivity().onBackPressedDispatcher.onBackPressed() },
                    onEdit = { navTo(MySharedAppsFragment.newInstance(), MySharedAppsFragment.TAG) },
                    onFavoriteClick = { clickAction(it) },
                    onBotClick = { app ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            botManagerViewModel.findUserByAppId(app.appId)?.let {
                                showUserBottom(parentFragmentManager, it, botEntrySource = AnalyticsTracker.BotSource.MORE_EXPLORE_DIALOG)
                            }
                        }
                    },
                )
            } else {
                val users by contacts.observeAsState(emptyList())
                val user by self.observeAsState(Session.getAccount()?.toUser())
                MorePage(
                    user = user,
                    contacts = users,
                    favorites = favoriteApps,
                    botCount = (favoriteApps + apps).distinctBy { it.appId }.size,
                    isDesktopLogin = isDesktopLogin,
                    clickedBotIds = clickedBotIds,
                    onProfile = { ProfileBottomSheetDialogFragment.newInstance().showNow(parentFragmentManager, ProfileBottomSheetDialogFragment.TAG) },
                    onQr = {
                        Session.getAccountId()?.let {
                            QrBottomSheetDialogFragment.newInstance(it, QrBottomSheetDialogFragment.TYPE_MY_QR)
                                .showNow(parentFragmentManager, QrBottomSheetDialogFragment.TAG)
                        }
                    },
                    onContacts = { navTo(ContactListFragment(), ContactListFragment.TAG) },
                    onContact = { showUserBottom(parentFragmentManager, it) },
                    onBots = { navTo(newInstance(bots = true), "ExploreBotsFragment") },
                    onBot = { clickAction(it) },
                )
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.homeToolbar.apply {
            setOnSearchClickListener {
                activity?.addFragment(this@ExploreFragment, SearchExploreFragment.newInstance(), SearchExploreFragment.TAG, id = R.id.internal_container)
            }
            setOnScanClickListener {
                RxPermissions(requireActivity()).request(Manifest.permission.CAMERA).autoDispose(stopScope).subscribe { granted ->
                    if (granted) (requireActivity() as? MainActivity)?.showCapture(true) else context?.openPermissionSetting()
                }
            }
            setOnSettingsClickListener { SettingActivity.show(requireContext(), compose = false) }
        }
        isDesktopLogin = Session.getExtensionSessionId() != null
        clickedBotIds = readClickedBotIds()
        loadData()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                botManagerViewModel.refreshFavoriteApps(requireNotNull(Session.getAccountId()))
                loadData()
            } catch (e: Exception) {
                ErrorHandler.handleError(e)
            }
        }
        RxBus.listen(BotEvent::class.java).observeOn(AndroidSchedulers.mainThread()).autoDispose(viewLifecycleOwner.scope(androidx.lifecycle.Lifecycle.Event.ON_DESTROY)).subscribe { loadData() }
        RxBus.listen(FavoriteEvent::class.java).observeOn(AndroidSchedulers.mainThread()).autoDispose(viewLifecycleOwner.scope(androidx.lifecycle.Lifecycle.Event.ON_DESTROY)).subscribe { loadData() }
        RxBus.listen(BadgeEvent::class.java).observeOn(AndroidSchedulers.mainThread()).autoDispose(viewLifecycleOwner.scope(androidx.lifecycle.Lifecycle.Event.ON_DESTROY)).subscribe {
            clickedBotIds = readClickedBotIds()
        }
        RxBus.listen(SessionEvent::class.java).observeOn(AndroidSchedulers.mainThread()).autoDispose(viewLifecycleOwner.scope(androidx.lifecycle.Lifecycle.Event.ON_DESTROY)).subscribe {
            isDesktopLogin = Session.getExtensionSessionId() != null
        }
    }

    private fun loadData() {
        viewLifecycleOwner.lifecycleScope.launch {
            favoriteApps = botManagerViewModel.getFavoriteAppsByUserId(requireNotNull(Session.getAccountId()))
            apps = botManagerViewModel.getAllExploreApps()
        }
    }

    private fun readClickedBotIds(): Set<String> =
        defaultSharedPreferences.getString(PREF_BOT_CLICKED_IDS, "").orEmpty().split(",").filter { it.isNotBlank() }.toSet()

    private fun setClickedBotId(id: String) {
        val ids = readClickedBotIds() + id
        defaultSharedPreferences.edit { putString(PREF_BOT_CLICKED_IDS, ids.joinToString(",")) }
        clickedBotIds = ids
        RxBus.publish(BadgeEvent(PREF_BOT_CLICKED_IDS))
    }

    private val clickAction: (BotInterface) -> Unit = { app ->
        if (app is ExploreApp) {
            lifecycleScope.launch {
                botManagerViewModel.findAppByAppId(app.appId)?.let { favoriteApp ->
                    AnalyticsTracker.trackOpenBotHomePage(AnalyticsTracker.BotSource.MORE_EXPLORE_FAVORITE, favoriteApp.appNumber)
                    WebActivity.show(requireActivity(), url = favoriteApp.homeUri, app = favoriteApp, conversationId = null)
                }
            }
        } else if (app is Bot) {
            if (SHOW_DOT_BOT_IDS.contains(app.id)) {
                setClickedBotId(app.id)
                clickedBotIds = readClickedBotIds()
            }
            when (app.id) {
                INTERNAL_LINK_DESKTOP_ID -> {
                    DeviceFragment.newInstance().showNow(parentFragmentManager, DeviceFragment.TAG)
                }
                INTERNAL_BUY_ID -> {
                    WalletActivity.showBuy(
                        requireActivity(),
                        false,
                        null,
                        null,
                        source = AnalyticsTracker.TradeSource.EXPLORE
                    )
                }
                INTERNAL_SWAP_ID -> {
                    val shown = RecoveryReminderBottomSheetDialogFragment.showForRiskAction(parentFragmentManager) {
                        AnalyticsTracker.trackTradeStart(TradeWallet.MAIN, AnalyticsTracker.TradeSource.EXPLORE)
                        SwapActivity.show(
                            requireActivity(),
                            null,
                            null,
                            null,
                            null,
                            entrySource = AnalyticsTracker.TradeSource.EXPLORE,
                            entryType = AnalyticsTracker.SpotTradeType.SIMPLE,
                        )
                    }
                    if (!shown) {
                        AnalyticsTracker.trackTradeStart(TradeWallet.MAIN, AnalyticsTracker.TradeSource.EXPLORE)
                        SwapActivity.show(
                            requireActivity(),
                            null,
                            null,
                            null,
                            null,
                            entrySource = AnalyticsTracker.TradeSource.EXPLORE,
                            entryType = AnalyticsTracker.SpotTradeType.SIMPLE,
                        )
                    }
                }
                INTERNAL_MEMBER_ID -> {
                    if (Session.getAccount()?.membership != null && Session.getAccount()?.membership?.plan != Plan.None) {
                        navTo(MixinMemberInvoicesFragment.newInstance(), MixinMemberInvoicesFragment.TAG)
                    } else {
                        MixinMemberUpgradeBottomSheetDialogFragment.newInstance().showNow(
                            parentFragmentManager, MixinMemberUpgradeBottomSheetDialogFragment.TAG
                        )
                    }
                }
                INTERNAL_REFERRAL_ID -> {
                    lifecycleScope.launch {
                        botManagerViewModel.findOrSyncApp(INTERNAL_REFERRAL_ID)?.let { app ->
                            setClickedBotId(INTERNAL_REFERRAL_ID)
                            WebActivity.show(requireActivity(), url = app.homeUri, app = app, conversationId = null)
                        }
                    }
                }
                INTERNAL_SUPPORT_ID -> {
                    lifecycleScope.launch {
                        val userTeamMixin = botManagerViewModel.refreshUser(Constants.TEAM_MIXIN_USER_ID)
                        if (userTeamMixin == null) {
                            toast(R.string.Data_error)
                        } else {
                            ConversationActivity.show(requireContext(), recipientId = Constants.TEAM_MIXIN_USER_ID)
                        }
                    }
                }
            }
        } else {
            lifecycleScope.launch {
                botManagerViewModel.findUserByAppId(app.getBotId())?.let { user ->
                    showUserBottom(parentFragmentManager, user, botEntrySource = AnalyticsTracker.BotSource.MORE_EXPLORE_FAVORITE)
                } ?: botManagerViewModel.findAppByAppId(app.getBotId())?.let { favoriteApp ->
                    WebActivity.show(requireActivity(), url = favoriteApp.homeUri, app = favoriteApp, conversationId = null)
                }
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        // UrlInterpreterActivity doesn't have a UI and needs it's son fragment to handle it's finish.
        if (activity is UrlInterpreterActivity) {
            var realFragmentCount = 0
            parentFragmentManager.fragments.forEach { _ ->
                realFragmentCount++
            }
            if (realFragmentCount <= 0) {
                activity?.finish()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
