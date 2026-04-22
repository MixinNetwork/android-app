package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.databinding.FragmentTransactionFiltersBinding
import one.mixin.android.job.MixinJobManager
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.CheckedFlowLayout
import javax.inject.Inject

abstract class BaseTransactionsFragment(
    @LayoutRes contentLayoutId: Int,
) : BaseFragment(contentLayoutId) {
    @Inject
    lateinit var jobManager: MixinJobManager

    protected val walletViewModel by viewModels<WalletViewModel>()

    private var transactionsRv: RecyclerView? = null

    private var _filterBinding: FragmentTransactionFiltersBinding? = null
    private val filterBinding get() = requireNotNull(_filterBinding)

    private var pagingJob: Job? = null

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

    protected fun <T : Any> bindPagingData(
        adapter: PagingDataAdapter<T, *>,
        flow: Flow<PagingData<T>>,
    ) {
        pagingJob?.cancel()
        pagingJob = viewLifecycleOwner.lifecycleScope.launch {
            flow.collectLatest { pagingData ->
                adapter.submitData(pagingData)
            }
        }
    }

    protected var currentType = R.id.filters_radio_all
    protected var currentOrder = R.id.sort_time

    abstract fun onApplyClick()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        transactionsRv = view.findViewById(R.id.transactions_rv)
        val transactionLayoutManager = LinearLayoutManager(requireContext())
        transactionsRv?.layoutManager = transactionLayoutManager

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

    override fun onDestroyView() {
        super.onDestroyView()
        _filterBinding = null
    }

    companion object {
        const val LIMIT = 30
    }
}
