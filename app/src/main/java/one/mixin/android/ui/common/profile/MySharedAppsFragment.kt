package one.mixin.android.ui.common.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_my_shared_apps.*
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.vo.App
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_my_shared_apps, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView.adapter = adapter
        recyclerView.addItemDecoration(SegmentationItemDecoration())
        title_view.leftIb.setOnClickListener { activity?.onBackPressed() }
        loadData()
        refresh()
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
            recyclerView ?: return@launch
            recyclerView.isVisible = favoriteApps.isNotEmpty() || unFavoriteApps.isNotEmpty()
            empty.isVisible = favoriteApps.isEmpty() && unFavoriteApps.isEmpty()
            adapter.setData(favoriteApps, unFavoriteApps)
        }
    }

    private val onAddSharedApp: (app: App) -> Unit = { app ->
        lifecycleScope.launch {
            val dialog = indeterminateProgressDialog(message = R.string.pb_dialog_message).apply {
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
    private val onRemoveSharedApp: (app: App) -> Unit = { app ->
        lifecycleScope.launch {
            val dialog = indeterminateProgressDialog(message = R.string.pb_dialog_message).apply {
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
