package one.mixin.android.ui.common.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_my_shared_apps.*
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.widget.recyclerview.OnStartDragListener
import one.mixin.android.widget.recyclerview.SimpleItemTouchHelperCallback
import javax.inject.Inject

class MySharedAppsFragment : BaseFragment(), OnStartDragListener {
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

    private val mItemTouchHelper: ItemTouchHelper by lazy {
        ItemTouchHelper(helperCallback)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mItemTouchHelper.attachToRecyclerView(list)
        list.adapter = adapter
        lifecycleScope.launch {
            val apps = mySharedAppsViewModel.getApps()
            adapter.setData(apps)
        }
    }

    private val helperCallback by lazy { SimpleItemTouchHelperCallback(adapter) }

    private val adapter by lazy { MySharedAppsAdapter(this) }

    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
        mItemTouchHelper.startDrag(viewHolder)
    }
}