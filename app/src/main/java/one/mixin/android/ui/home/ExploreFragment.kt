package one.mixin.android.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
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
import one.mixin.android.databinding.ItemFavoriteDecorationBinding
import one.mixin.android.databinding.ItemFavoriteEditBinding
import one.mixin.android.event.BotEvent
import one.mixin.android.event.FavoriteEvent
import one.mixin.android.event.SessionEvent
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.notEmptyWithElse
import one.mixin.android.extension.openPermissionSetting
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
import one.mixin.android.ui.search.SearchBotsFragment
import one.mixin.android.ui.url.UrlInterpreterActivity
import one.mixin.android.ui.wallet.WalletActivity
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
            searchIv.setOnClickListener {
                activity?.addFragment(
                    this@ExploreFragment,
                    SearchBotsFragment(),
                    SearchBotsFragment.TAG,
                )
            }
            scanIv.setOnClickListener {
                RxPermissions(requireActivity()).request(Manifest.permission.CAMERA).autoDispose(stopScope).subscribe { granted ->
                    if (granted) {
                        (requireActivity() as? MainActivity)?.showCapture(true)
                    } else {
                        context?.openPermissionSetting()
                    }
                }
            }
            favoriteRv.adapter = adapter
            favoriteRv.addItemDecoration(SegmentationItemDecoration())
            radioGroupExplore.setOnCheckedChangeListener { _, checkedId ->
                when (checkedId) {
                    R.id.radio_favorite -> {
                        exploreVa.displayedChild = 0
                    }

                    R.id.radio_bot -> {
                        exploreVa.displayedChild = 1
                    }
                }
            }
            binding.botRv.layoutManager = LinearLayoutManager(requireContext())
            binding.botRv.adapter = botsAdapter
            botRv.adapter = botsAdapter

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

    private fun loadData() {
        lifecycleScope.launch {
            val favoriteApps = botManagerViewModel.getFavoriteAppsByUserId(Session.getAccountId()!!)
            adapter.setData(favoriteApps)
        }
    }

    private val adapter by lazy {
        FavoriteAdapter({
            activity?.addFragment(
                this@ExploreFragment,
                MySharedAppsFragment.newInstance(),
                MySharedAppsFragment.TAG,
            )
        }, { app ->
            clickAction(app)
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
            if (apps.isEmpty()) {
                binding.emptyFl.isVisible = true
                binding.botRv.isVisible = false
            } else {
                binding.emptyFl.isVisible = false
                binding.botRv.isVisible = true
            }
            botsAdapter.list = apps
        }
    }

    private val botsAdapter by lazy {
        BotAdapter(clickAction)
    }

    private val clickAction: (BotInterface) -> Unit = { app ->
        if (app is ExploreApp) {
            lifecycleScope.launch {
                botManagerViewModel.findUserByAppId(app.appId)?.let { user ->
                    showUserBottom(parentFragmentManager, user)
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

    class FavoriteAdapter(private val editAction: () -> Unit, private val botAction: (BotInterface) -> Unit) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private var favoriteApps: List<ExploreApp>? = null

        var isDesktopLogin = false
            set(value) {
                if (value == field) return

                field = value
                notifyItemChanged(InternalBots.indexOf(InternalLinkDesktop))
            }

        @SuppressLint("NotifyDataSetChanged")
        fun setData(
            favoriteApps: List<ExploreApp>
        ) {
            this.favoriteApps = favoriteApps
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): RecyclerView.ViewHolder {
            return when (viewType) {
                2 -> {
                    FavoriteEditHolder(ItemFavoriteEditBinding.inflate(LayoutInflater.from(parent.context), parent, false))
                }
                3 -> {
                    FavoriteDecorationHolder(ItemFavoriteDecorationBinding.inflate(LayoutInflater.from(parent.context), parent, false))
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
            if (getItemViewType(position) == 2) {
                holder.itemView.setOnClickListener {
                    editAction.invoke()
                }
            } else if(getItemViewType(position) != 3){
                getItem(position)?.let { app ->
                    (holder as FavoriteHolder).bind(app, isDesktopLogin)
                    holder.itemView.setOnClickListener {
                        botAction.invoke(app)
                    }
                }
            }
        }

        override fun getItemCount(): Int {
            return InternalBots.size + (favoriteApps?.size ?: 0) + 2
        }

        override fun getItemViewType(position: Int): Int {
            return if (position < InternalBots.size) {
                1
            } else if (position == itemCount - 1) {
                2
            } else if (position == InternalBots.size) {
                3
            } else 0
        }

        fun getItem(position: Int): BotInterface? {
            return if (position < InternalBots.size) {
                InternalBots[position]
            } else {
                favoriteApps?.get(position - InternalBots.size - 1)
            }
        }

    }

    class FavoriteHolder(private val itemBinding: ItemFavoriteBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(
            app: BotInterface?,
            isDesktopLogin: Boolean,
        ) {
            app ?: return
            if (app is Bot){
                val a = if (app == InternalLinkDesktop && isDesktopLogin) {
                    InternalLinkDesktopLogged
                } else app
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
                    verifiedIv.setImageResource(if(app.isVerified == true) R.drawable.ic_bot else R.drawable.ic_user_verified)
                }
            }
        }
    }

    class FavoriteEditHolder(itemBinding: ItemFavoriteEditBinding) : RecyclerView.ViewHolder(itemBinding.root)
    class FavoriteDecorationHolder(itemBinding: ItemFavoriteDecorationBinding) : RecyclerView.ViewHolder(itemBinding.root)

    class BotAdapter(private val botCallBack: (ExploreApp) -> Unit) : RecyclerView.Adapter<BotAdapter.ListViewHolder>() {
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): ListViewHolder {
            val view =
                LayoutInflater.from(
                    parent.context,
                ).inflate(R.layout.item_favorite, parent, false)
            return ListViewHolder(view)
        }

        var list: List<ExploreApp> = listOf()
            @SuppressLint("NotifyDataSetChanged")
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun onBindViewHolder(
            holder: ListViewHolder,
            position: Int,
        ) {
            val binding = ItemFavoriteBinding.bind(holder.itemView)
            list[position].let { app ->
                binding.avatar.renderApp(app)
                binding.name.text = app.name
                binding.mixinIdTv.text = app.appNumber
                binding.verifiedIv.setImageResource(if(app.isVerified == true) R.drawable.ic_bot else R.drawable.ic_user_verified)
                holder.itemView.setOnClickListener {
                    botCallBack.invoke(app)
                }
                binding.avatar.setOnClickListener {
                    botCallBack.invoke(app)
                }
                binding.avatar.tag = position
            }
        }

        override fun getItemCount(): Int {
            return list.notEmptyWithElse({ it.size }, 0)
        }

        class ListViewHolder(itemView: View?) : RecyclerView.ViewHolder(itemView!!)
    }
}
