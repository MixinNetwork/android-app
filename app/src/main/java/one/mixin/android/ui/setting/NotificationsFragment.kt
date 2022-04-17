package one.mixin.android.ui.setting

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.InputType
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants.Account.PREF_DUPLICATE_TRANSFER
import one.mixin.android.Constants.Account.PREF_STRANGER_TRANSFER
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.AccountUpdateRequest
import one.mixin.android.databinding.FragmentNotificationsBinding
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.openNotificationSetting
import one.mixin.android.extension.supportsOreo
import one.mixin.android.extension.toast
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.editDialog
import one.mixin.android.util.ChannelManager
import one.mixin.android.util.PropertyHelper
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Fiats

@AndroidEntryPoint
class NotificationsFragment : BaseFragment(R.layout.fragment_notifications) {
    companion object {
        const val TAG = "NotificationsFragment"
        fun newInstance(): NotificationsFragment {
            return NotificationsFragment()
        }
    }

    private val accountSymbol = Fiats.getSymbol()

    private val viewModel by viewModels<SettingViewModel>()
    private val binding by viewBinding(FragmentNotificationsBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            titleView.leftIb.setOnClickListener { activity?.onBackPressed() }
            transferRl.setOnClickListener {
                showDialog(transferTv.text.toString().removePrefix(accountSymbol), true)
            }
            refreshNotification(Session.getAccount()!!.transferNotificationThreshold)
            systemNotification.setOnClickListener {
                context?.openNotificationSetting()
            }

            largeAmountRl.setOnClickListener {
                showDialog(Session.getAccount()!!.transferConfirmationThreshold.toString(), false)
            }
            refreshLargeAmount(Session.getAccount()!!.transferConfirmationThreshold)

            lifecycleScope.launch {
                duplicateTransferSc.isChecked = PropertyHelper.findValueByKey(PREF_DUPLICATE_TRANSFER)?.toBoolean() ?: true
                strangerTransferSc.isChecked = PropertyHelper.findValueByKey(PREF_STRANGER_TRANSFER)?.toBoolean() ?: true
            }
            duplicateTransferSc.setOnCheckedChangeListener { _, isChecked ->
                updateKeyValue(PREF_DUPLICATE_TRANSFER, isChecked.toString())
            }

            strangerTransferSc.setOnCheckedChangeListener { _, isChecked ->
                updateKeyValue(PREF_STRANGER_TRANSFER, isChecked.toString())
            }

            supportsOreo {
                notificationReset.isVisible = true
                notificationReset.setOnClickListener {
                    ChannelManager.resetChannelSound(requireContext())
                    toast(R.string.Successful)
                }
            }
        }
    }

    private fun updateKeyValue(key: String, value: String) = lifecycleScope.launch {
        PropertyHelper.updateKeyValue(key, value)
    }

    @SuppressLint("RestrictedApi")
    private fun showDialog(amount: String?, isNotification: Boolean) {
        if (context == null || !isAdded) {
            return
        }
        editDialog {
            titleText = this@NotificationsFragment.getString(
                if (isNotification) {
                    R.string.Transfer_Amount_count_down
                } else {
                    R.string.wallet_transaction_tip_title_with_symbol
                },
                accountSymbol
            )
            editText = amount
            editHint = this@NotificationsFragment.getString(
                if (isNotification) {
                    R.string.Transfer_Amount
                } else {
                    R.string.Large_Amount_Confirmation
                }
            )
            editInputType = InputType.TYPE_NUMBER_FLAG_DECIMAL + InputType.TYPE_CLASS_NUMBER
            allowEmpty = false
            rightAction = {
                val result = it.toDoubleOrNull()
                if (result == null) {
                    toast(R.string.Data_error)
                } else {
                    savePreference(it.toDouble(), isNotification)
                }
            }
        }
    }

    private fun savePreference(threshold: Double, isNotification: Boolean) = lifecycleScope.launch {
        val pb = indeterminateProgressDialog(
            message = R.string.Please_wait_a_bit,
            title = if (isNotification) R.string.Transfer_Notifications else R.string.Large_Amount_Confirmation
        ).apply {
            setCancelable(false)
        }
        pb.show()

        handleMixinResponse(
            invokeNetwork = {
                viewModel.preferences(
                    if (isNotification) {
                        AccountUpdateRequest(
                            fiatCurrency = Session.getFiatCurrency(),
                            transferNotificationThreshold = threshold
                        )
                    } else {
                        AccountUpdateRequest(
                            fiatCurrency = Session.getFiatCurrency(),
                            transferConfirmationThreshold = threshold
                        )
                    }
                )
            },
            successBlock = {
                it.data?.let { account ->
                    Session.storeAccount(account)
                    if (isNotification) {
                        refreshNotification(account.transferNotificationThreshold)
                    } else {
                        refreshLargeAmount(account.transferConfirmationThreshold)
                    }
                }
            },
            doAfterNetworkSuccess = {
                pb.dismiss()
            },
            exceptionBlock = {
                pb.dismiss()
                return@handleMixinResponse false
            }
        )
    }

    @SuppressLint("SetTextI18n")
    private fun refreshNotification(threshold: Double) {
        if (viewDestroyed()) return
        binding.apply {
            transferTv.text = "$accountSymbol$threshold"
            transferDescTv.text = getString(
                R.string.setting_notification_transfer_desc,
                "$accountSymbol$threshold"
            )
        }
    }

    @SuppressLint("SetTextI18n")
    private fun refreshLargeAmount(largeAmount: Double) {
        if (viewDestroyed()) return
        binding.apply {
            largeAmountTv.text = "$accountSymbol$largeAmount"
            if (largeAmount <= 0.0) {
                largeAmountDescTv.text = getString(R.string.setting_transfer_large_summary_greater, "${accountSymbol}0")
            } else {
                largeAmountDescTv.text = getString(R.string.setting_transfer_large_summary, "$accountSymbol$largeAmount")
            }
        }
    }
}
