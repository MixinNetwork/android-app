package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_transaction_filters.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.R
import one.mixin.android.job.MixinJobManager
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.widget.BottomSheet
import javax.inject.Inject

abstract class BaseTransactionsFragment<C> : BaseFragment() {

    @Inject
    lateinit var jobManager: MixinJobManager
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    protected val walletViewModel: WalletViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(WalletViewModel::class.java)
    }

    protected var offset = 0L
    protected val limit = 100
    private var transactionsRv: RecyclerView? = null
    protected var initialLoadKey: Int? = null

    protected lateinit var dataObserver: Observer<C>

    protected fun showFiltersSheet() {
        filtersView.filters_radio_group.setCheckedById(currentType)
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
        view.filters_title.left_ib.setOnClickListener { filtersSheet.dismiss() }
        setRadioGroupListener(view)
        view
    }

    private var currentLiveData: LiveData<C>? = null

    protected fun bindLiveData(liveData: LiveData<C>) {
        currentLiveData?.removeObserver(dataObserver)
        currentLiveData = liveData
        currentLiveData?.observe(this, dataObserver)
    }

    protected var currentType = R.id.filters_radio_all

    abstract fun setRadioGroupListener(view: View)
    abstract fun refreshSnapshots()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        transactionsRv = view?.findViewById(R.id.transactions_rv)
        val transactionLayoutManager = LinearLayoutManager(requireContext())
        transactionsRv?.layoutManager = transactionLayoutManager
        transactionsRv?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val lastPos = transactionLayoutManager.findLastVisibleItemPosition()
                if (lastPos >= offset) {
                    refreshSnapshots()
                    offset += limit
                }
            }
        })
    }

    override fun onStop() {
        super.onStop()
        initialLoadKey = (transactionsRv?.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
    }
}