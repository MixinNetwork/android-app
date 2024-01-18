package one.mixin.android.ui.home

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.manager.SupportRequestManagerFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.databinding.FragmentExploreBinding
import one.mixin.android.databinding.ItemFavoriteBinding
import one.mixin.android.databinding.ItemSharedAppBinding
import one.mixin.android.databinding.ItemSharedLocalAppBinding
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.job.TipCounterSyncedLiveData
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.profile.MySharedAppsViewModel
import one.mixin.android.ui.common.profile.holder.FooterHolder
import one.mixin.android.ui.common.profile.holder.ItemViewHolder
import one.mixin.android.ui.common.profile.holder.LocalAppHolder
import one.mixin.android.ui.common.profile.holder.SharedAppHolder
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
        }
        loadData()
        refresh()
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
