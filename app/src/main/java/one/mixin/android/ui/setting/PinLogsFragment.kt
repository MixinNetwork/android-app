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
            fun description(
                titleRes: Int,
                subtitleRes: Int,
            ) = Pair(context.getString(titleRes), context.getString(subtitleRes))

            when (code) {
                "VERIFICATION" ->
                    return description(R.string.pin_log_title_verify_pin, R.string.pin_log_subtitle_pin_incorrect)
                "RAW_TRANSFER" ->
                    return description(R.string.pin_log_title_transfer, R.string.pin_log_subtitle_pin_incorrect)
                "USER_TRANSFER" ->
                    return description(R.string.pin_log_title_transfer, R.string.pin_log_subtitle_pin_incorrect)
                "WITHDRAWAL" ->
                    return description(R.string.pin_log_title_withdrawal, R.string.pin_log_subtitle_pin_incorrect)
                "ADD_ADDRESS" ->
                    return description(R.string.pin_log_title_add_address, R.string.pin_log_subtitle_pin_incorrect)
                "DELETE_ADDRESS" ->
                    return description(R.string.pin_log_title_delete_address, R.string.pin_log_subtitle_pin_incorrect)
                "ADD_EMERGENCY" ->
                    return description(R.string.pin_log_title_add_recovery_contact, R.string.pin_log_subtitle_pin_incorrect)
                "DELETE_EMERGENCY" ->
                    return description(R.string.pin_log_title_delete_recovery_contact, R.string.pin_log_subtitle_pin_incorrect)
                "READ_EMERGENCY" ->
                    return description(R.string.pin_log_title_view_recovery_contact, R.string.pin_log_subtitle_pin_incorrect)
                "UPDATE_PHONE" ->
                    return description(R.string.pin_log_title_update_mobile_number, R.string.pin_log_subtitle_pin_incorrect)
                "UPDATE_PIN" ->
                    return description(R.string.pin_log_title_change_pin, R.string.pin_log_subtitle_pin_incorrect)
                "MULTISIG_SIGN" ->
                    return description(R.string.pin_log_title_sign_multisig_transaction, R.string.pin_log_subtitle_pin_incorrect)
                "MULTISIG_UNLOCK" ->
                    return description(R.string.pin_log_title_revoke_multisig_transaction, R.string.pin_log_subtitle_pin_incorrect)
                "LOGIN_FROM_DESKTOP" ->
                    return description(R.string.pin_log_title_sign_in_on_desktop, R.string.pin_log_subtitle_pin_incorrect)
                "COLLECTIBLE_SIGN" ->
                    return description(R.string.pin_log_title_inscribe_collectible, R.string.pin_log_subtitle_pin_incorrect)
                "COLLECTIBLE_UNLOCK" ->
                    return description(R.string.pin_log_title_release_collectible, R.string.pin_log_subtitle_pin_incorrect)
                "DO_AUTHORIZATION" ->
                    return description(R.string.pin_log_title_authorize_bot, R.string.pin_log_subtitle_pin_incorrect)
                "APP_OWNERSHIP_TRANSFER" ->
                    return description(R.string.pin_log_title_transfer_bot_ownership, R.string.pin_log_subtitle_pin_incorrect)
                "DELETE_ACCOUNT" ->
                    return description(R.string.pin_log_title_delete_account, R.string.pin_log_subtitle_pin_incorrect)
                "ACTIVITY_PIN_CREATION" ->
                    return description(R.string.pin_log_title_set_pin, R.string.pin_log_subtitle_pin_set)
                "ACTIVITY_PIN_MODIFICATION" ->
                    return description(R.string.pin_log_title_change_pin, R.string.pin_log_subtitle_pin_changed)
                "ACTIVITY_EMERGENCY_CONTACT_MODIFICATION" ->
                    return description(R.string.pin_log_title_change_recovery_contact, R.string.pin_log_subtitle_recovery_contact_changed)
                "ACTIVITY_PHONE_MODIFICATION" ->
                    return description(R.string.pin_log_title_change_mobile_number, R.string.pin_log_subtitle_mobile_number_changed)
                "ACTIVITY_LOGIN_BY_PHONE" ->
                    return description(R.string.pin_log_title_sign_in_on_mobile, R.string.pin_log_subtitle_signed_in_via_mobile_number)
                "ACTIVITY_LOGIN_BY_EMERGENCY_CONTACT" ->
                    return description(R.string.pin_log_title_sign_in_on_mobile, R.string.pin_log_subtitle_signed_in_via_recovery_contact)
                "ACTIVITY_LOGIN_BY_MNEMONIC" ->
                    return description(R.string.pin_log_title_sign_in_on_mobile, R.string.pin_log_subtitle_signed_in_via_mnemonic_phrase)
                "ACTIVITY_LOGIN_FROM_DESKTOP" ->
                    return description(R.string.pin_log_title_sign_in_on_desktop, R.string.pin_log_subtitle_signed_in_on_desktop)
                "ACTIVITY_LOGOUT_PHONE" ->
                    return description(R.string.pin_log_title_sign_out_on_mobile, R.string.pin_log_subtitle_signed_out_on_mobile)
                "ACTIVITY_LOGOUT_DESKTOP" ->
                    return description(R.string.pin_log_title_sign_out_on_desktop, R.string.pin_log_subtitle_signed_out_on_desktop)
                "USER_EXPORT_PRIVATE" ->
                    return description(R.string.pin_log_title_export_mnemonic_phrase, R.string.pin_log_subtitle_mnemonic_phrase_exported)
                "UPGRADE_SAFE" ->
                    return description(R.string.pin_log_title_upgrade_safe, R.string.pin_log_subtitle_account_upgraded)
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
