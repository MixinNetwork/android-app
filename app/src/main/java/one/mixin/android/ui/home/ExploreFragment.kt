package one.mixin.android.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.manager.SupportRequestManagerFragment
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.databinding.FragmentExploreBinding
import one.mixin.android.databinding.ItemFavoriteBinding
import one.mixin.android.databinding.ItemFavoriteEditBinding
import one.mixin.android.databinding.ItemFavoriteTitleBinding
import one.mixin.android.event.BotEvent
import one.mixin.android.event.FavoriteEvent
import one.mixin.android.event.SessionEvent
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.notEmptyWithElse
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.putInt
import one.mixin.android.extension.toast
import one.mixin.android.job.TipCounterSyncedLiveData
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.profile.MySharedAppsFragment
import one.mixin.android.ui.common.showUserBottom
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.device.DeviceFragment
import one.mixin.android.ui.home.bot.Bot
import one.mixin.android.ui.home.bot.BotManagerViewModel
import one.mixin.android.ui.home.bot.INTERNAL_BUY_ID
import one.mixin.android.ui.home.bot.INTERNAL_CAMERA_ID
import one.mixin.android.ui.home.bot.INTERNAL_LINK_DESKTOP_ID
import one.mixin.android.ui.home.bot.INTERNAL_SUPPORT_ID
import one.mixin.android.ui.home.bot.InternalBots
import one.mixin.android.ui.home.bot.InternalLinkDesktop
import one.mixin.android.ui.home.bot.InternalLinkDesktopLogged
import one.mixin.android.ui.home.web3.EthereumFragment
import one.mixin.android.ui.search.SearchBotsFragment
import one.mixin.android.ui.setting.SettingActivity
import one.mixin.android.ui.url.UrlInterpreterActivity
import one.mixin.android.ui.wallet.WalletActivity
import one.mixin.android.ui.web.WebActivity
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.rxpermission.RxPermissions
import one.mixin.android.vo.BotInterface
import one.mixin.android.vo.ExploreApp
import one.mixin.android.widget.SegmentationItemDecoration
import javax.inject.Inject

@AndroidEntryPoint
class ExploreFragment : BaseFragment() {
    companion object {
        const val TAG = "ExploreFragment"

        fun newInstance() = ExploreFragment()
    }

    @Inject
    lateinit var tipCounterSynced: TipCounterSyncedLiveData

    private var _binding: FragmentExploreBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val botManagerViewModel by viewModels<BotManagerViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentExploreBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("RestrictedApi")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            root.setOnClickListener {
                // do nothing
            }
            searchIb.setOnClickListener {
                activity?.addFragment(
                    this@ExploreFragment,
                    SearchBotsFragment(),
                    SearchBotsFragment.TAG,
                )
            }
            scanIb.setOnClickListener {
                RxPermissions(requireActivity()).request(Manifest.permission.CAMERA).autoDispose(stopScope).subscribe { granted ->
                    if (granted) {
                        (requireActivity() as? MainActivity)?.showCapture(true)
                    } else {
                        context?.openPermissionSetting()
                    }
                }
            }
            settingIb.setOnClickListener {
                SettingActivity.show(requireContext(), compose = false)
            }
            favoriteRv.adapter = adapter
            favoriteRv.addItemDecoration(SegmentationItemDecoration())

            if (defaultSharedPreferences.getInt(Constants.Account.PREF_EXPLORE_SELECT, 0) == 0) {
                exploreVa.displayedChild = 0
                radioFavorite.isChecked = true
                radioEth.isChecked = false
            } else {
                exploreVa.displayedChild = 1
                radioEth.isChecked = true
                radioFavorite.isChecked = false
                navigate(ethereumFragment, EthereumFragment.TAG)
            }

            radioGroupExplore.setOnCheckedChangeListener { _, checkedId ->
                when (checkedId) {
                    R.id.radio_favorite -> {
                        defaultSharedPreferences.putInt(Constants.Account.PREF_EXPLORE_SELECT, 0)
                        exploreVa.displayedChild = 0
                    }

                    R.id.radio_eth -> {
                        defaultSharedPreferences.putInt(Constants.Account.PREF_EXPLORE_SELECT, 1)
                        exploreVa.displayedChild = 1
                        navigate(ethereumFragment, EthereumFragment.TAG)
                    }

                }
            }

            adapter.isDesktopLogin = Session.getExtensionSessionId() != null
        }
        loadData()
        loadBotData()
        refresh()

        RxBus.listen(BotEvent::class.java).observeOn(AndroidSchedulers.mainThread()).autoDispose(destroyScope).subscribe {
            loadBotData()
        }
        RxBus.listen(FavoriteEvent::class.java).observeOn(AndroidSchedulers.mainThread()).autoDispose(destroyScope).subscribe {
            loadData()
        }
        RxBus.listen(SessionEvent::class.java).observeOn(AndroidSchedulers.mainThread()).autoDispose(destroyScope).subscribe {
            adapter.isDesktopLogin = Session.getExtensionSessionId() != null
        }
    }

    private fun navigate(
        destinationFragment: Fragment,
        tag: String,
    ) {
        val tx = parentFragmentManager.beginTransaction()
        val f = parentFragmentManager.findFragmentByTag(tag)
        if (f == null) {
            tx.add(R.id.fragment_container, destinationFragment, tag)
            destinations.add(destinationFragment)
        } else {
            tx.show(f)
        }
        destinations.forEach {
            if (it.tag != tag) {
                tx.hide(it)
            }
        }
        tx.commitAllowingStateLoss()
    }

    private val destinations = mutableListOf<Fragment>()
    private val ethereumFragment by lazy {
        EthereumFragment()
    }

    private fun loadData() {
        lifecycleScope.launch {
            val favoriteApps = botManagerViewModel.getFavoriteAppsByUserId(Session.getAccountId()!!)
            adapter.setData(favoriteApps)
        }
    }

    private val adapter by lazy {
        BotAdapter({
            activity?.addFragment(
                this@ExploreFragment,
                MySharedAppsFragment.newInstance(),
                MySharedAppsFragment.TAG,
            )
        }, { app ->
            clickAction(app)
        }, { bot ->
            lifecycleScope.launch {
                botManagerViewModel.findUserByAppId(bot.getBotId())?.let { user ->
                    showUserBottom(parentFragmentManager, user)
                }
            }
        })
    }

    private fun refresh() {
        lifecycleScope.launch {
            try {
                botManagerViewModel.refreshFavoriteApps(Session.getAccountId()!!)
                loadData()
            } catch (e: Exception) {
                ErrorHandler.handleError(e)
            }
        }
    }

    private fun loadBotData() {
        lifecycleScope.launch {
            val apps = botManagerViewModel.getAllExploreApps()
            adapter.list = apps
        }
    }

    private val clickAction: (BotInterface) -> Unit = { app ->
        if (app is ExploreApp) {
            lifecycleScope.launch {
                botManagerViewModel.findAppByAppId(app.appId)?.let { app ->
                    WebActivity.show(requireActivity(), url = app.homeUri, app = app, conversationId = null)
                }
            }
        } else if (app is Bot) {
            when (app.id) {
                INTERNAL_CAMERA_ID -> {
                    RxPermissions(requireActivity()).request(Manifest.permission.CAMERA).autoDispose(stopScope).subscribe { granted ->
                        if (granted) {
                            (requireActivity() as? MainActivity)?.showCapture(false)
                        } else {
                            context?.openPermissionSetting()
                        }
                    }
                }

                INTERNAL_BUY_ID -> {
                    WalletActivity.showBuy(requireActivity(), null, null)
                }

                INTERNAL_LINK_DESKTOP_ID -> {
                    DeviceFragment.newInstance().showNow(parentFragmentManager, DeviceFragment.TAG)
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
            // do nothing
        }
    }

    override fun onDetach() {
        super.onDetach()
        // UrlInterpreterActivity doesn't have a UI and needs it's son fragment to handle it's finish.
        if (activity is UrlInterpreterActivity) {
            var realFragmentCount = 0
            parentFragmentManager.fragments.forEach { f ->
                if (f !is SupportRequestManagerFragment) {
                    realFragmentCount++
                }
            }
            if (realFragmentCount <= 0) {
                activity?.finish()
            }
        }
    }

    class BotAdapter(private val editAction: () -> Unit, private val botAction: (BotInterface) -> Unit, private val appAction: (BotInterface) -> Unit) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private val TYPE_INTERNAL = 0
        private val TYPE_TITEL = 1
        private val TYPE_FAVORITE = 2
        private val TYPE_EDIT = 3
        private val TYPE_BOT = 4

        private var favoriteApps: List<ExploreApp>? = null
        var list: List<ExploreApp> = listOf()
            @SuppressLint("NotifyDataSetChanged")
            set(value) {
                field = value
                notifyDataSetChanged()
            }
        var isDesktopLogin = false
            set(value) {
                if (value == field) return

                field = value
                notifyItemChanged(InternalBots.indexOf(InternalLinkDesktop))
            }

        @SuppressLint("NotifyDataSetChanged")
        fun setData(
            favoriteApps: List<ExploreApp>,
        ) {
            this.favoriteApps = favoriteApps
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): RecyclerView.ViewHolder {
            return when (viewType) {
                TYPE_EDIT -> {
                    FavoriteEditHolder(ItemFavoriteEditBinding.inflate(LayoutInflater.from(parent.context), parent, false))
                }

                TYPE_TITEL -> {
                    FavoriteTitleHolder(ItemFavoriteTitleBinding.inflate(LayoutInflater.from(parent.context), parent, false))
                }

                else -> {
                    FavoriteHolder(ItemFavoriteBinding.inflate(LayoutInflater.from(parent.context), parent, false))
                }
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onBindViewHolder(
            holder: RecyclerView.ViewHolder,
            position: Int,
        ) {
            val type = getItemViewType(position)
            if (type == TYPE_EDIT) {
                holder.itemView.setOnClickListener {
                    editAction.invoke()
                }
            } else if (type == TYPE_INTERNAL || type == TYPE_FAVORITE) {
                getItem(type, position)?.let { app ->
                    (holder as FavoriteHolder).bind(app, isDesktopLogin)
                    holder.itemView.setOnClickListener {
                        botAction.invoke(app)
                    }
                }
            } else if (type == TYPE_BOT) {
                getItem(type, position)?.let { bot ->
                    (holder as FavoriteHolder).bind(bot, isDesktopLogin)
                    holder.itemView.setOnClickListener {
                        appAction.invoke(bot)
                    }
                }
            }
        }

        override fun getItemCount(): Int {
            return InternalBots.size + (favoriteApps?.size ?: 0) + list.notEmptyWithElse({ it.size }, 0) + 2
        }

        override fun getItemViewType(position: Int): Int {
            return if (position < InternalBots.size) {
                TYPE_INTERNAL
            } else if (position == InternalBots.size) {
                TYPE_TITEL
            } else if (position < InternalBots.size + (favoriteApps?.size ?: 0) + 1) {
                TYPE_FAVORITE
            } else if (position == InternalBots.size + (favoriteApps?.size ?: 0) + 1) {
                TYPE_EDIT
            } else {
                TYPE_BOT
            }
        }

        private fun getItem(type: Int, position: Int): BotInterface? {
            return if (type == TYPE_INTERNAL) {
                InternalBots[position]
            } else if (type == TYPE_FAVORITE) {
                favoriteApps?.get(position - InternalBots.size - 1)
            } else {
                list.get(position - InternalBots.size - (favoriteApps?.size ?: 0) - 2)
            }
        }
    }

    class FavoriteHolder(private val itemBinding: ItemFavoriteBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(
            app: BotInterface?,
            isDesktopLogin: Boolean,
        ) {
            app ?: return
            if (app is Bot) {
                val a =
                    if (app == InternalLinkDesktop && isDesktopLogin) {
                        InternalLinkDesktopLogged
                    } else {
                        app
                    }
                itemBinding.apply {
                    avatar.renderApp(a)
                    name.setText(a.name)
                    mixinIdTv.setText(a.description)
                    verifiedIv.isVisible = false
                }
            } else if (app is ExploreApp) {
                itemBinding.apply {
                    avatar.setInfo(app.name, app.iconUrl, app.appId)
                    name.text = app.name
                    mixinIdTv.text = app.appNumber
                    verifiedIv.isVisible = true
                    verifiedIv.setImageResource(if (app.isVerified == true) R.drawable.ic_bot else R.drawable.ic_user_verified)
                }
            }
        }
    }

    class FavoriteEditHolder(itemBinding: ItemFavoriteEditBinding) : RecyclerView.ViewHolder(itemBinding.root)

    class FavoriteTitleHolder(itemBinding: ItemFavoriteTitleBinding) : RecyclerView.ViewHolder(itemBinding.root)
}
