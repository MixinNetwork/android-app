package one.mixin.android.ui.setting

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_notifications.*
import kotlinx.android.synthetic.main.view_title.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.AccountUpdateRequest
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.openNotificationSetting
import one.mixin.android.extension.supportsOreo
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseViewModelFragment
import one.mixin.android.ui.common.editDialog
import one.mixin.android.util.ChannelManager
import one.mixin.android.util.Session
import one.mixin.android.vo.Fiats

@AndroidEntryPoint
class NotificationsFragment : BaseViewModelFragment<SettingViewModel>() {
    companion object {
        const val TAG = "NotificationsFragment"
        fun newInstance(): NotificationsFragment {
            return NotificationsFragment()
        }
    }

    private val accountSymbol = Fiats.getSymbol()

    override fun getModelClass() = SettingViewModel::class.java

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_notifications, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }
        transfer_rl.setOnClickListener {
            showDialog(transfer_tv.text.toString().removePrefix(accountSymbol), true)
        }
        refreshNotification(Session.getAccount()!!.transferNotificationThreshold)
        system_notification.setOnClickListener {
            context?.openNotificationSetting()
        }

        large_amount_rl.setOnClickListener {
            showDialog(Session.getAccount()!!.transferConfirmationThreshold.toString(), false)
        }
        refreshLargeAmount(Session.getAccount()!!.transferConfirmationThreshold)
        supportsOreo {
            notification_reset.isVisible = true
            notification_reset.setOnClickListener {
                ChannelManager.resetChannelSound(requireContext())
                toast(R.string.successful)
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private fun showDialog(amount: String?, isNotification: Boolean) {
        if (context == null || !isAdded) {
            return
        }
        editDialog {
            titleText = this@NotificationsFragment.getString(
                if (isNotification) {
                    R.string.setting_notification_transfer_amount
                } else {
                    R.string.wallet_transaction_tip_title_with_symbol
                },
                accountSymbol
            )
            editText = amount
            editHint = this@NotificationsFragment.getString(
                if (isNotification) {
                    R.string.wallet_transfer_amount
                } else {
                    R.string.wallet_transaction_tip_title
                }
            )
            editInputType = InputType.TYPE_NUMBER_FLAG_DECIMAL + InputType.TYPE_CLASS_NUMBER
            allowEmpty = false
            rightAction = {
                savePreference(it.toDouble(), isNotification)
            }
        }
    }

    private fun savePreference(threshold: Double, isNotification: Boolean) = lifecycleScope.launch {
        val pb = indeterminateProgressDialog(
            message = R.string.pb_dialog_message,
            title = if (isNotification) R.string.setting_notification_transfer else R.string.wallet_transaction_tip_title
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
            switchContext = Dispatchers.IO,
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
        if (!isAdded) return
        transfer_tv.text = "$accountSymbol$threshold"
        transfer_desc_tv.text = getString(
            R.string.setting_notification_transfer_desc,
            "$accountSymbol$threshold"
        )
    }

    @SuppressLint("SetTextI18n")
    private fun refreshLargeAmount(largeAmount: Double) {
        if (!isAdded) return
        large_amount_tv.text = "$accountSymbol$largeAmount"
        large_amount_desc_tv.text = getString(
            R.string.setting_transfer_large_summary,
            "$accountSymbol$largeAmount"
        )
    }
}
