package one.mixin.android.ui.common.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.fragment_my_shared_apps.*
import kotlinx.android.synthetic.main.view_title.view.*
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.util.Session
import one.mixin.android.vo.App
import javax.inject.Inject

class MySharedAppsFragment : BaseFragment() {
    companion object {
        const val TAG = "MySharedAppsFragment"
        fun newInstance(): MySharedAppsFragment {
            return MySharedAppsFragment()
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val mySharedAppsViewModel: MySharedAppsViewModel by viewModels { viewModelFactory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_my_shared_apps, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        list.adapter = adapter
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }
        loadData()
        refresh()
    }

    private fun refresh() {
        lifecycleScope.launch {
            mySharedAppsViewModel.refreshFavoriteApps(Session.getAccountId()!!)
            loadData()
        }
    }

    private fun loadData() {
        lifecycleScope.launch {
            val favoriteApps =
                mySharedAppsViewModel.getFavoriteAppsByUserId(Session.getAccountId()!!)
            val unFavoriteApps = mySharedAppsViewModel.getUnfavoriteApps()
            adapter.setData(favoriteApps, unFavoriteApps)
        }
    }

    private val onAddSharedApp: (app: App) -> Unit = { app ->
        lifecycleScope.launch {
            if (mySharedAppsViewModel.addFavoriteApp(app.appId)) {
                loadData()
            }
        }
    }
    private val onRemoveSharedApp: (app: App) -> Unit = { app ->
        lifecycleScope.launch {
            if (mySharedAppsViewModel.removeFavoriteApp(app.appId, Session.getAccountId()!!)) {
                loadData()
            }
        }
    }

    private val adapter by lazy { MySharedAppsAdapter(onAddSharedApp, onRemoveSharedApp) }
}
