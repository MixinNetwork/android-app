package one.mixin.android.ui.common.profile

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.databinding.FragmentMySharedAppsBinding
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.ExploreApp
import one.mixin.android.widget.SegmentationItemDecoration

@AndroidEntryPoint
class MySharedAppsFragment : BaseFragment() {
    companion object {
        const val TAG = "MySharedAppsFragment"

        fun newInstance(): MySharedAppsFragment {
            return MySharedAppsFragment()
        }
    }

    private val mySharedAppsViewModel by viewModels<MySharedAppsViewModel>()
    private val binding by viewBinding(FragmentMySharedAppsBinding::bind)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.fragment_my_shared_apps, container, false)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            recyclerView.adapter = adapter
            recyclerView.addItemDecoration(SegmentationItemDecoration())
            titleView.leftIb.setOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
        }
        loadData()
        refresh()
        binding.searchEt.et.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int,
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int,
                ) {
                }

                override fun afterTextChanged(s: Editable) {
                    keyword = s.toString()
                }
            },
        )
    }

    private var keyword: String = ""
        set(value) {
            if (field == value) return
            field = value
            loadData()
        }

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

    private fun loadData() {
        lifecycleScope.launch {
            val favoriteApps =
                mySharedAppsViewModel.getFavoriteAppsByUserId(Session.getAccountId()!!)
            val unFavoriteApps = mySharedAppsViewModel.getUnfavoriteApps()
            if (keyword.isNotBlank()) {
                val filterFavoriteApps = favoriteApps.filter { app -> app.name.contains(keyword, ignoreCase = true) || app.appNumber.contains(keyword, ignoreCase = true) }
                val filterUnFavoriteApps = unFavoriteApps.filter { app -> app.name.contains(keyword, ignoreCase = true) || app.appNumber.contains(keyword, ignoreCase = true) }
                adapter.setData(filterFavoriteApps, filterUnFavoriteApps, keyword)
                binding.empty.isVisible = adapter.isEmpty()
                binding.emptyTv.isInvisible = true
                binding.emptyTitle.setText(R.string.NO_RESULTS)
                binding.recyclerView.isVisible = !adapter.isEmpty()
            } else {
                adapter.setData(favoriteApps, unFavoriteApps)
                binding.empty.isVisible = adapter.isEmpty()
                binding.emptyTv.isInvisible = false
                binding.emptyTitle.setText(R.string.NO_BOTS)
                binding.recyclerView.isVisible = !adapter.isEmpty()
            }
        }
    }

    private val onAddSharedApp: (app: ExploreApp) -> Unit = { app ->
        lifecycleScope.launch {
            val dialog =
                indeterminateProgressDialog(message = R.string.Please_wait_a_bit).apply {
                    setCancelable(false)
                }
            try {
                if (mySharedAppsViewModel.addFavoriteApp(app.appId)) {
                    loadData()
                }
            } catch (e: Exception) {
                ErrorHandler.handleError(e)
            }
            dialog.dismiss()
        }
    }
    private val onRemoveSharedApp: (app: ExploreApp) -> Unit = { app ->
        lifecycleScope.launch {
            val dialog =
                indeterminateProgressDialog(message = R.string.Please_wait_a_bit).apply {
                    setCancelable(false)
                }
            try {
                if (mySharedAppsViewModel.removeFavoriteApp(app.appId, Session.getAccountId()!!)) {
                    loadData()
                }
            } catch (e: Exception) {
                ErrorHandler.handleError(e)
            }
            dialog.dismiss()
        }
    }

    private val adapter by lazy { MySharedAppsAdapter(onAddSharedApp, onRemoveSharedApp) }
}
