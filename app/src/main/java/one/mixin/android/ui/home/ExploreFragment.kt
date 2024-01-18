package one.mixin.android.ui.home

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.manager.SupportRequestManagerFragment
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.databinding.FragmentExploreBinding
import one.mixin.android.databinding.ItemFavoriteBinding
import one.mixin.android.event.BotEvent
import one.mixin.android.job.TipCounterSyncedLiveData
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.profile.MySharedAppsViewModel
import one.mixin.android.ui.home.bot.BotManagerAdapter
import one.mixin.android.ui.url.UrlInterpreterActivity
import one.mixin.android.util.ErrorHandler
import one.mixin.android.vo.App
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

    private val mySharedAppsViewModel by viewModels<MySharedAppsViewModel>()

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
            root.setOnClickListener {  }
            searchIv.setOnClickListener {  }
            scanIv.setOnClickListener {  }
            icCamera.setOnClickListener {  }
            icBuy.setOnClickListener {  }
            icDesktop.setOnClickListener {  }
            icSupport.setOnClickListener {  }
            favoriteRv.adapter = adapter
            favoriteRv.addItemDecoration(SegmentationItemDecoration())
            radioGroupExplore.setOnCheckedChangeListener { _, checkedId ->
                when(checkedId){
                    R.id.radio_favorite -> {
                        exploreVa.displayedChild = 0
                    }
                    R.id.radio_bot -> {
                        exploreVa.displayedChild = 1
                    }
                }
            }
            binding.botRv.layoutManager = GridLayoutManager(requireContext(), 4)
            binding.botRv.adapter = bottomListAdapter
            botRv.adapter = bottomListAdapter
        }
        loadData()
        loadBotData()
        refresh()

        RxBus.listen(BotEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(destroyScope)
            .subscribe {
                loadBotData()
            }
    }

    private fun loadData() {
        lifecycleScope.launch {
            val favoriteApps =
                mySharedAppsViewModel.getFavoriteAppsByUserId(Session.getAccountId()!!)
            adapter.setData(favoriteApps)
        }
    }

    private val adapter by lazy { FavoriteAdapter() }
    private fun refresh() {
        lifecycleScope.launch {
            try {
                mySharedAppsViewModel.refreshFavoriteApps(Session.getAccountId()!!)
                loadData()
            } catch (e: Exception) {
                ErrorHandler.handleError(e)
            }
        }
    }

    private fun loadBotData() {
        lifecycleScope.launch {
            val apps = mySharedAppsViewModel.getAllApps()
            if (apps.isEmpty()) {
                binding.emptyFl.isVisible = true
                binding.botRv.isVisible = false
            } else {
                binding.emptyFl.isVisible = false
                binding.botRv.isVisible = true
            }
            bottomListAdapter.list = apps
        }
    }

    private val bottomListAdapter by lazy {
        BotManagerAdapter({ id-> })
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

    class FavoriteAdapter : RecyclerView.Adapter<FavoriteHolder>() {
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
        ): FavoriteHolder {
            return FavoriteHolder(ItemFavoriteBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onBindViewHolder(
            holder: FavoriteHolder,
            position: Int,
        ) {
            holder.bind(getItem(position))
        }

        override fun getItemCount(): Int {
            return favoriteApps?.size ?: 0
        }

        fun getItem(position: Int): App? {
            return favoriteApps?.get(position)
        }
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
}
