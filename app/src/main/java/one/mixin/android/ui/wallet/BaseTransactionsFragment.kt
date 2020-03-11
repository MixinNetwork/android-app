package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_transaction_filters.view.*
import kotlinx.android.synthetic.main.view_round_title.view.*
import one.mixin.android.R
import one.mixin.android.job.MixinJobManager
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.CheckedFlowLayout

abstract class BaseTransactionsFragment<C> : BaseFragment() {

    @Inject
    lateinit var jobManager: MixinJobManager
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    protected val walletViewModel: WalletViewModel by viewModels { viewModelFactory }

    private var localDataSizeChanged = true
    protected var localDataSize: Int = 0
        set(value) {
            if (field != value) {
                field = value
                localDataSizeChanged = true
            }
        }
    private var transactionsRv: RecyclerView? = null
    protected var initialLoadKey: Int? = null

    protected lateinit var dataObserver: Observer<C>
    protected var refreshedSnapshots: Boolean = false

    protected fun showFiltersSheet() {
        filtersView.sort_flow.setCheckedById(currentOrder)
        filtersView.filter_flow.setCheckedById(currentType)
        filtersSheet.show()
    }

    protected val filtersSheet: BottomSheet by lazy {
        val builder = BottomSheet.Builder(requireActivity())
        val bottomSheet = builder.create()
        builder.setCustomView(filtersView)
        bottomSheet
    }

    private val filtersView: View by lazy {
        val view = View.inflate(ContextThemeWrapper(context, R.style.Custom), R.layout.fragment_transaction_filters, null)
        view.filters_title.right_iv.setOnClickListener { filtersSheet.dismiss() }
        view.apply_tv.setOnClickListener { onApplyClick() }
        view.filter_flow.setOnCheckedListener(object : CheckedFlowLayout.OnCheckedListener {
            override fun onChecked(id: Int) {
                currentType = id
            }
        })
        view.sort_flow.setOnCheckedListener(object : CheckedFlowLayout.OnCheckedListener {
            override fun onChecked(id: Int) {
                currentOrder = id
            }
        })
        view
    }

    private var currentLiveData: LiveData<C>? = null

    protected fun bindLiveData(liveData: LiveData<C>) {
        currentLiveData?.removeObserver(dataObserver)
        currentLiveData = liveData
        currentLiveData?.observe(viewLifecycleOwner, dataObserver)
    }

    protected var currentType = R.id.filters_radio_all
    protected var currentOrder = R.id.sort_time

    abstract fun refreshSnapshots()
    abstract fun onApplyClick()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        transactionsRv = view?.findViewById(R.id.transactions_rv)
        val transactionLayoutManager = LinearLayoutManager(requireContext())
        transactionsRv?.layoutManager = transactionLayoutManager
        transactionsRv?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val lastPos = transactionLayoutManager.findLastVisibleItemPosition()
                if (localDataSizeChanged && lastPos >= localDataSize - 1 && lastPos >= LIMIT - 1) {
                    localDataSizeChanged = false
                    refreshSnapshots()
                }
            }
        })
    }

    override fun onStop() {
        super.onStop()
        initialLoadKey = (transactionsRv?.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
    }

    companion object {
        const val LIMIT = 50
    }
}
