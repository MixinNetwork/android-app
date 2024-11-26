package one.mixin.android.ui.setting

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.databinding.FragmentPinLogsBinding
import one.mixin.android.databinding.ItemPinLogsBinding
import one.mixin.android.extension.formatToLocalTime
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.LogResponse

@AndroidEntryPoint
class PinLogsFragment : BaseFragment(R.layout.fragment_pin_logs) {
    private val viewModel by viewModels<SettingViewModel>()
    private val binding by viewBinding(FragmentPinLogsBinding::bind)

    companion object {
        const val TAG = "PinLogsFragment"

        fun newInstance() = PinLogsFragment()

        fun getLogDescription(
            context: Context,
            code: String,
        ): Pair<String, String> {
            when (code) {
                "VERIFICATION" ->
                    return Pair(context.getString(R.string.PIN_incorrect), context.getString(R.string.Verify))
                "RAW_TRANSFER" ->
                    return Pair(context.getString(R.string.PIN_incorrect), context.getString(R.string.Raw_Transfer))
                "USER_TRANSFER" ->
                    return Pair(context.getString(R.string.PIN_incorrect), context.getString(R.string.Transfer))
                "WITHDRAWAL" ->
                    return Pair(context.getString(R.string.PIN_incorrect), context.getString(R.string.Withdrawal))
                "ADD_ADDRESS" ->
                    return Pair(context.getString(R.string.PIN_incorrect), context.getString(R.string.Add_address))
                "DELETE_ADDRESS" ->
                    return Pair(context.getString(R.string.PIN_incorrect), context.getString(R.string.Delete_address))
                "ADD_EMERGENCY" ->
                    return Pair(context.getString(R.string.PIN_incorrect), context.getString(R.string.Add_emergency_contact))
                "DELETE_EMERGENCY" ->
                    return Pair(context.getString(R.string.PIN_incorrect), context.getString(R.string.Delete_emergency_contact))
                "READ_EMERGENCY" ->
                    return Pair(context.getString(R.string.PIN_incorrect), context.getString(R.string.View_emergency_contact))
                "UPDATE_PHONE" ->
                    return Pair(context.getString(R.string.PIN_incorrect), context.getString(R.string.Change_Phone_Number))
                "UPDATE_PIN" ->
                    return Pair(context.getString(R.string.PIN_incorrect), context.getString(R.string.PIN_change))
                "MULTISIG_SIGN" ->
                    return Pair(context.getString(R.string.PIN_incorrect), context.getString(R.string.Multisig_Transaction))
                "MULTISIG_UNLOCK" ->
                    return Pair(context.getString(R.string.PIN_incorrect), context.getString(R.string.Revoke_multisig_transaction))
                "ACTIVITY_PIN_CREATION" ->
                    return Pair(context.getString(R.string.pin_creation), context.getString(R.string.your_pin_has_been_created))
                "ACTIVITY_PIN_MODIFICATION" ->
                    return Pair(context.getString(R.string.PIN_change), context.getString(R.string.PIN_change))
                "ACTIVITY_EMERGENCY_CONTACT_MODIFICATION" ->
                    return Pair(context.getString(R.string.Emergency_Contact), context.getString(R.string.Change_emergency_contact))
                "ACTIVITY_PHONE_MODIFICATION" ->
                    return Pair(context.getString(R.string.Phone_number_change), context.getString(R.string.Phone_number_change))
                "ACTIVITY_LOGIN_BY_PHONE" ->
                    return Pair(context.getString(R.string.Sign_in), context.getString(R.string.Sign_with_mobile_number))
                "ACTIVITY_LOGIN_BY_EMERGENCY_CONTACT" ->
                    return Pair(context.getString(R.string.Sign_in), context.getString(R.string.Sign_with_emergency_contact))
                "ACTIVITY_LOGIN_FROM_DESKTOP" ->
                    return Pair(context.getString(R.string.Sign_in), context.getString(R.string.Sign_in_desktop_app))
                "USER_EXPORT_PRIVATE" ->
                    return Pair(context.getString(R.string.Export), context.getString(R.string.Export_mnemonic_phrase))
                "UPGRADE_SAFE" ->
                    return Pair(context.getString(R.string.Upgrade), context.getString(R.string.Upgrade_safe))
                else ->
                    return Pair(code, code)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            title.leftIb.setOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
            list.adapter = adapter
            list.setOnScrollChangeListener { _, _, _, _, _ ->
                if (isAdded) {
                    if (!list.canScrollVertically(1)) {
                        loadMore()
                    }
                }
            }
            lifecycleScope.launch {
                isLoading = true
                handleMixinResponse(
                    invokeNetwork = {
                        viewModel.getPinLogs()
                    },
                    successBlock = { result ->
                        hasMore = result.data?.isNotEmpty() == true
                        empty.isVisible = false
                        list.isVisible = true
                        adapter.data.addAll(result.data!!)
                        adapter.notifyDataSetChanged()
                        isLoading = false
                        progress.isVisible = false
                    },
                    defaultExceptionHandle = {
                        hasMore = false
                        empty.isVisible = true
                        list.isVisible = false
                        isLoading = false
                        progress.isVisible = false
                        ErrorHandler.handleError(it)
                    },
                )
            }
        }
    }

    private var hasMore = false
    private var isLoading = false

    @SuppressLint("NotifyDataSetChanged")
    private fun loadMore() {
        if (isLoading || !hasMore) {
            return
        }
        isLoading = true
        lifecycleScope.launch {
            handleMixinResponse(
                invokeNetwork = {
                    viewModel.getPinLogs(adapter.data.last().createdAt)
                },
                successBlock = { result ->
                    adapter.data.addAll(result.data!!)
                    adapter.notifyDataSetChanged()
                    isLoading = false
                },
                defaultExceptionHandle = {
                    hasMore = false
                    isLoading = false
                    ErrorHandler.handleError(it)
                },
            )
        }
    }

    private val adapter by lazy {
        PinAdapter()
    }

    class PinHolder(private val itemBinding: ItemPinLogsBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(pin: LogResponse) {
            val result = getLogDescription(itemView.context, pin.code)
            itemBinding.apply {
                logTitle.text = result.first
                logDesc.text = result.second
                logCreated.text = pin.createdAt.formatToLocalTime()
                logAddress.text = pin.ipAddress
            }
        }
    }

    class PinAdapter : RecyclerView.Adapter<PinHolder>() {
        var data: MutableList<LogResponse> = mutableListOf()

        override fun getItemCount(): Int = data.size

        fun getItem(position: Int) = data.get(position)

        override fun onBindViewHolder(
            holder: PinHolder,
            position: Int,
        ) {
            getItem(position).let {
                holder.bind(it)
            }
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): PinHolder =
            PinHolder(
                ItemPinLogsBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            )
    }
}
