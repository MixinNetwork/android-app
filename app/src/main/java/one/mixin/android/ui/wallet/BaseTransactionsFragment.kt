package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.uber.autodispose.autoDispose
import io.reactivex.android.schedulers.AndroidSchedulers
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.databinding.FragmentTransactionFiltersBinding
import one.mixin.android.event.RefreshSnapshotEvent
import one.mixin.android.job.MixinJobManager
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.CheckedFlowLayout
import javax.inject.Inject

abstract class BaseTransactionsFragment<C>(
    @LayoutRes contentLayoutId: Int,
) : BaseFragment(contentLayoutId) {
    @Inject
    lateinit var jobManager: MixinJobManager

    protected val walletViewModel by viewModels<WalletViewModel>()

    protected var refreshPosition = 0
    protected var refreshOffset: String? = null
    protected var lastRefreshOffset: String? = null

    private var transactionsRv: RecyclerView? = null
    protected var initialLoadKey: Int? = null

    protected lateinit var dataObserver: Observer<C>

    private var _filterBinding: FragmentTransactionFiltersBinding? = null
    private val filterBinding get() = requireNotNull(_filterBinding)

    protected fun showFiltersSheet() {
        filterBinding.apply {
            sortFlow.setCheckedById(currentOrder)
            filterFlow.setCheckedById(currentType)
            filtersSheet.show()
        }
    }

    protected val filtersSheet: BottomSheet by lazy {
        val builder = BottomSheet.Builder(requireActivity())
        val bottomSheet = builder.create()
        builder.setCustomView(filterBinding.root)
        bottomSheet
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RxBus.listen(RefreshSnapshotEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(destroyScope)
            .subscribe { event ->
                refreshOffset = event.lastCreatedAt
            }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        transactionsRv = view.findViewById(R.id.transactions_rv)
        val transactionLayoutManager = LinearLayoutManager(requireContext())
        transactionsRv?.layoutManager = transactionLayoutManager
        transactionsRv?.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                override fun onScrolled(
                    recyclerView: RecyclerView,
                    dx: Int,
                    dy: Int,
                ) {
                    val lastPos = transactionLayoutManager.findLastVisibleItemPosition()
                    if (lastPos >= refreshPosition + LIMIT - 1 && lastRefreshOffset != refreshOffset) {
                        refreshPosition += LIMIT - 1
                        refreshSnapshots()
                        lastRefreshOffset = refreshOffset
                    }
                }
            },
        )

        _filterBinding = FragmentTransactionFiltersBinding.bind(View.inflate(ContextThemeWrapper(context, R.style.Custom), R.layout.fragment_transaction_filters, null))
        filterBinding.apply {
            filtersTitle.rightIv.setOnClickListener { filtersSheet.dismiss() }
            applyTv.setOnClickListener { onApplyClick() }
            filterFlow.setOnCheckedListener(
                object : CheckedFlowLayout.OnCheckedListener {
                    override fun onChecked(id: Int) {
                        currentType = id
                    }
                },
            )
            sortFlow.setOnCheckedListener(
                object : CheckedFlowLayout.OnCheckedListener {
                    override fun onChecked(id: Int) {
                        currentOrder = id
                    }
                },
            )
        }
        filterBinding.root
    }

    override fun onStop() {
        super.onStop()
        initialLoadKey = (transactionsRv?.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _filterBinding = null
    }

    companion object {
        const val LIMIT = 30
    }
}
