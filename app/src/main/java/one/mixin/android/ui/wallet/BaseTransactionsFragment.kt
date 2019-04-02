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
import one.mixin.android.widget.RadioGroup
import javax.inject.Inject

abstract class BaseTransactionsFragment<C> : BaseFragment() {

    @Inject
    lateinit var jobManager: MixinJobManager
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    protected val walletViewModel: WalletViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(WalletViewModel::class.java)
    }

    protected var lastCreatedAt: String? = null
    private var uiOffset = 0L
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
        view.filters_radio_group.setOnCheckedListener(object : RadioGroup.OnCheckedListener {
            override fun onChecked(id: Int) {
                currentType = id
                refreshWithCurrentType()
                filtersSheet.dismiss()
            }
        })
        view
    }

    private var currentLiveData: LiveData<C>? = null

    protected fun bindLiveData(liveData: LiveData<C>) {
        currentLiveData?.removeObserver(dataObserver)
        currentLiveData = liveData
        currentLiveData?.observe(this, dataObserver)
    }

    protected var currentType = R.id.filters_radio_all

    abstract fun refreshWithCurrentType()
    abstract fun refreshSnapshots()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        transactionsRv = view?.findViewById(R.id.transactions_rv)
        val transactionLayoutManager = LinearLayoutManager(requireContext())
        transactionsRv?.layoutManager = transactionLayoutManager
        transactionsRv?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val lastPos = transactionLayoutManager.findLastVisibleItemPosition()
                if (lastPos >= uiOffset) {
                    refreshSnapshots()
                    uiOffset += LIMIT
                }
            }
        })
    }

    override fun onStop() {
        super.onStop()
        initialLoadKey = (transactionsRv?.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
    }

    companion object {
        const val PAGE_SIZE = 33
        const val LIMIT = 99
    }
}