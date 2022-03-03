package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.uber.autodispose.autoDispose
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import io.reactivex.android.schedulers.AndroidSchedulers
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.databinding.FragmentTransactionFiltersBinding
import one.mixin.android.databinding.FragmentTranscationExportBinding
import one.mixin.android.event.RefreshSnapshotEvent
import one.mixin.android.extension.isNightMode
import one.mixin.android.job.MixinJobManager
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.CheckedFlowLayout
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject

abstract class BaseTransactionsFragment<C> : BaseFragment() {

    @Inject
    lateinit var jobManager: MixinJobManager

    protected val walletViewModel by viewModels<WalletViewModel>()

    protected var refreshPosition = 0
    protected var refreshOffset: String? = null
    protected var lastRefreshOffset: String? = null

    private var transactionsRv: RecyclerView? = null
    protected var initialLoadKey: Int? = null

    protected lateinit var dataObserver: Observer<C>
    protected var refreshedSnapshots: Boolean = false

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

    private var _exportBinding: FragmentTranscationExportBinding? = null
    private val exportBinding get() = requireNotNull(_exportBinding)

    protected fun showExportSheet() {
        exportBinding.apply {
            exportSheet.show()
        }
    }

    protected val exportSheet: BottomSheet by lazy {
        val builder = BottomSheet.Builder(requireActivity())
        val bottomSheet = builder.create()
        builder.setCustomView(exportBinding.root)
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

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        transactionsRv = view.findViewById(R.id.transactions_rv)
        val transactionLayoutManager = LinearLayoutManager(requireContext())
        transactionsRv?.layoutManager = transactionLayoutManager
        transactionsRv?.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    val lastPos = transactionLayoutManager.findLastVisibleItemPosition()
                    if (lastPos >= refreshPosition + LIMIT - 1 && lastRefreshOffset != refreshOffset) {
                        refreshPosition += LIMIT - 1
                        refreshSnapshots()
                        lastRefreshOffset = refreshOffset
                    }
                }
            }
        )

        _exportBinding = FragmentTranscationExportBinding.bind(View.inflate(ContextThemeWrapper(context, R.style.Custom), R.layout.fragment_transcation_export, null))
        exportBinding.apply {
            exportTitle.rightIv.setOnClickListener { exportSheet.dismiss() }
            exportStart.setOnClickListener {
                DatePickerDialog.newInstance { _, year, monthOfYear, dayOfMonth ->
                    exportStartTv.text = "$year-$monthOfYear-$dayOfMonth"
                    Timber.e("$year $monthOfYear $dayOfMonth")
                    startDate = Calendar.getInstance().apply {
                        set(year,monthOfYear,dayOfMonth)
                    }
                }.apply {
                    isThemeDark = isNightMode
                    maxDate = if (this@BaseTransactionsFragment.endDate != null) {
                        this@BaseTransactionsFragment.endDate
                    } else {
                        Calendar.getInstance()
                    }
                }.show(parentFragmentManager, "date")
            }
            exportEnd.setOnClickListener {
                DatePickerDialog.newInstance { _, year, monthOfYear, dayOfMonth ->
                    exportEndTv.text = "$year-$monthOfYear-$dayOfMonth"
                    Timber.e("$year $monthOfYear $dayOfMonth")
                    endDate = Calendar.getInstance().apply {
                        set(year,monthOfYear,dayOfMonth)
                    }
                }.apply {
                    isThemeDark = isNightMode
                    if (this@BaseTransactionsFragment.startDate != null) {
                        minDate = this@BaseTransactionsFragment.startDate
                    }
                    maxDate = Calendar.getInstance()
                }.show(parentFragmentManager, "date")
            }
        }

        _filterBinding = FragmentTransactionFiltersBinding.bind(View.inflate(ContextThemeWrapper(context, R.style.Custom), R.layout.fragment_transaction_filters, null))
        filterBinding.apply {
            filtersTitle.rightIv.setOnClickListener { filtersSheet.dismiss() }
            applyTv.setOnClickListener { onApplyClick() }
            filterFlow.setOnCheckedListener(
                object : CheckedFlowLayout.OnCheckedListener {
                    override fun onChecked(id: Int) {
                        currentType = id
                    }
                }
            )
            sortFlow.setOnCheckedListener(
                object : CheckedFlowLayout.OnCheckedListener {
                    override fun onChecked(id: Int) {
                        currentOrder = id
                    }
                }
            )
        }
        filterBinding.root
    }

    private val isNightMode by lazy {
        requireContext().isNightMode()
    }

    private var startDate:Calendar? =null
    private var endDate:Calendar? =null

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
