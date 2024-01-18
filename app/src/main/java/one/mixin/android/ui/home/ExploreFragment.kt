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
import androidx.recyclerview.widget.GridLayoutManager
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
import one.mixin.android.event.BotEvent
import one.mixin.android.extension.addFragment
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
import one.mixin.android.ui.home.bot.BotManagerAdapter
import one.mixin.android.ui.home.bot.BotManagerViewModel
import one.mixin.android.ui.home.bot.INTERNAL_CAMERA_ID
import one.mixin.android.ui.home.bot.INTERNAL_SCAN_ID
import one.mixin.android.ui.home.bot.INTERNAL_SUPPORT_ID
import one.mixin.android.ui.url.UrlInterpreterActivity
import one.mixin.android.ui.wallet.WalletActivity
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.rxpermission.RxPermissions
import one.mixin.android.vo.App
import one.mixin.android.vo.BotInterface
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
            icCamera.setOnClickListener {
                RxPermissions(requireActivity()).request(Manifest.permission.CAMERA).autoDispose(stopScope).subscribe { granted ->
                    if (granted) {
                        (requireActivity() as? MainActivity)?.showCapture(false)
                    } else {
                        context?.openPermissionSetting()
                    }
                }
            }
            icBuy.setOnClickListener {
                WalletActivity.showBuy(requireActivity(), null, null)
            }
            icDesktop.setOnClickListener {
                DeviceFragment.newInstance().showNow(parentFragmentManager, DeviceFragment.TAG)
            }
            icSupport.setOnClickListener {
                lifecycleScope.launch {
                    val userTeamMixin = botManagerViewModel.refreshUser(Constants.TEAM_MIXIN_USER_ID)
                    if (userTeamMixin == null) {
                        toast(R.string.Data_error)
                    } else {
                        ConversationActivity.show(requireContext(), recipientId = Constants.TEAM_MIXIN_USER_ID)
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
            binding.botRv.layoutManager = GridLayoutManager(requireContext(), 4)
            binding.botRv.adapter = botsAdapter
            botRv.adapter = botsAdapter
        }
        loadData()
        loadBotData()
        refresh()

        RxBus.listen(BotEvent::class.java).observeOn(AndroidSchedulers.mainThread()).autoDispose(destroyScope).subscribe {
            loadBotData()
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
            val apps = botManagerViewModel.getAllApps()
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
        BotManagerAdapter(clickAction)
    }

    private val clickAction: (BotInterface) -> Unit = { app ->
        if (app is App) {
            lifecycleScope.launch {
                botManagerViewModel.findUserByAppId(app.appId)?.let { user ->
                    showUserBottom(parentFragmentManager, user)
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

    class FavoriteAdapter(private val editAction: () -> Unit, private val botAction: (App) -> Unit) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private var favoriteApps: List<App>? = null

        @SuppressLint("NotifyDataSetChanged")
        fun setData(
            favoriteApps: List<App>
        ) {
            this.favoriteApps = favoriteApps
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): RecyclerView.ViewHolder {
            return if (viewType == 1) {
                FavoriteEditHolder(ItemFavoriteEditBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            } else {
                FavoriteHolder(ItemFavoriteBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onBindViewHolder(
            holder: RecyclerView.ViewHolder,
            position: Int,
        ) {
            if (getItemViewType(position) == 1) {
                holder.itemView.setOnClickListener {
                    editAction.invoke()
                }
            } else {
                getItem(position)?.let { app ->
                    (holder as FavoriteHolder).bind(app)
                    holder.itemView.setOnClickListener {
                        botAction.invoke(app)
                    }
                }
            }
        }

        override fun getItemCount(): Int {
            return (favoriteApps?.size ?: 0) + if (showEdit()) 1 else 0
        }

        override fun getItemViewType(position: Int): Int {
            return if (position >= (favoriteApps?.size ?: 0)) 1
            else 0
        }

        fun getItem(position: Int): App? {
            return favoriteApps?.get(position)
        }

        private fun showEdit() = (favoriteApps?.size ?: 0) < 5
    }

    class FavoriteHolder(private val itemBinding: ItemFavoriteBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(
            app: App?
        ) {
            app ?: return
            itemBinding.apply {
                avatar.setInfo(app.name, app.iconUrl, app.appId)
                name.text = app.name
                mixinIdTv.text = app.appNumber
            }
        }
    }

    class FavoriteEditHolder(private val itemBinding: ItemFavoriteEditBinding) : RecyclerView.ViewHolder(itemBinding.root)
}
