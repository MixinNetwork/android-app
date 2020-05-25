package one.mixin.android.ui.setting

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_pin_logs.*
import kotlinx.android.synthetic.main.item_pin_logs.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.extension.localTime
import one.mixin.android.ui.common.BaseViewModelFragment
import one.mixin.android.vo.PINLogResponse

class PinLogsFragment : BaseViewModelFragment<SettingViewModel>() {
    override fun getModelClass() = SettingViewModel::class.java

    companion object {
        const val TAG = "PinLogsFragment"
        private const val PAGE_COUNT = 100
        fun newInstance() = PinLogsFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_pin_logs, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title.left_ib.setOnClickListener { activity?.onBackPressed() }
        list.adapter = adapter
        list.setOnScrollChangeListener { _, _, _, _, _ ->
            if (isAdded) {
                if (!list.canScrollVertically(1)) {
                    loadMore()
                }
            }
        }
        viewModel.viewModelScope.launch {
            isLoading = true
            val result = viewModel.getPinLogs()
            if (result.isSuccess && result.data?.isNotEmpty() == true) {
                hasMore = result.data?.size!! >= PAGE_COUNT
                empty.isVisible = false
                list.isVisible = true
                adapter.data.addAll(result.data!!)
                adapter.notifyDataSetChanged()
            } else {
                hasMore = false
                empty.isVisible = true
                list.isVisible = false
            }
            isLoading = false
            progress.isVisible = false
        }
    }

    private var hasMore = false
    private var isLoading = false
    private var page = 0
    private fun loadMore() {
        if (isLoading && !hasMore) {
            return
        }
        isLoading = true
        viewModel.viewModelScope.launch {
            val result = viewModel.getPinLogs((page + 1) * PAGE_COUNT)
            if (result.isSuccess && result.data?.isNotEmpty() == true) {
                page += 1
                hasMore = result.data?.size!! >= PAGE_COUNT
                adapter.data.addAll(result.data!!)
            } else {
                hasMore = false
            }
            isLoading = false
        }
    }

    private val adapter by lazy {
        PinAdapter()
    }

    class PinHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        @SuppressLint("SetTextI18n")
        fun bind(pin: PINLogResponse) {
            itemView.log_title.text =
                getLogDescription(itemView.context, pin.code)
            itemView.log_created.text = pin.createdAt.localTime()
            itemView.log_desc.text = pin.ipAddress
        }

        private fun getLogDescription(context: Context, code: String): String {
            when (code) {
                "VERIFICATION" ->
                    return context.getString(R.string.pin_verification)
                "RAW_TRANSFER" ->
                    return context.getString(R.string.pin_raw_transfer)
                "USER_TRANSFER" ->
                    return context.getString(R.string.pin_user_transfer)
                "WITHDRAWAL" ->
                    return context.getString(R.string.pin_withdrawal)
                "ADD_ADDRESS" ->
                    return context.getString(R.string.pin_add_address)
                "DELETE_ADDRESS" ->
                    return context.getString(R.string.pin_delete_address)
                "ADD_EMERGENCY" ->
                    return context.getString(R.string.pin_add_emergency)
                "DELETE_EMERGENCY" ->
                    return context.getString(R.string.pin_delete_emergency)
                "READ_EMERGENCY" ->
                    return context.getString(R.string.pin_read_emergency)
                "UPDATE_PHONE" ->
                    return context.getString(R.string.pin_update_phone)
                "UPDATE_PIN" ->
                    return context.getString(R.string.pin_update_pin)
                "MULTISIG_SIGN" ->
                    return context.getString(R.string.pin_log_multisig_sign)
                "MULTISIG_UNLOCK" ->
                    return context.getString(R.string.pin_log_multisig_unlock)
                "PIN_MODIFICATION" ->
                    return context.getString(R.string.pin_log_modification)
                "EMERGENCY_CONTACT_MODIFICATION" ->
                    return context.getString(R.string.pin_log_emergency_modification)
                "PHONE_MODIFICATION" ->
                    return context.getString(R.string.pin_log_phone_modification)
                "LOGIN_BY_PHONE" ->
                    return context.getString(R.string.pin_log_login_phone)
                "LOGIN_BY_EMERGENCY_CONTACT" ->
                    return context.getString(R.string.pin_log_login_emergency)
                "LOGIN_DESKTOP" ->
                    return context.getString(R.string.pin_log_login_desktop)
                else ->
                    return code
            }
        }
    }

    class PinAdapter : RecyclerView.Adapter<PinHolder>() {
        var data: MutableList<PINLogResponse> = mutableListOf()
        override fun getItemCount(): Int = data.size

        fun getItem(position: Int) = data.get(position)
        override fun onBindViewHolder(holder: PinHolder, position: Int) {
            getItem(position).let {
                holder.bind(it)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PinHolder = PinHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_pin_logs,
                parent,
                false
            )
        )
    }
}
