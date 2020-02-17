package one.mixin.android.ui.setting

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.fragment_notifications.*
import kotlinx.android.synthetic.main.view_title.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.AccountUpdateRequest
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.openNotificationSetting
import one.mixin.android.extension.removeEnd
import one.mixin.android.ui.common.BaseViewModelFragment
import one.mixin.android.ui.common.editDialog
import one.mixin.android.util.Session
import one.mixin.android.vo.Fiats

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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }
        transfer_rl.setOnClickListener {
            showDialog(transfer_tv.text.toString().removeEnd(accountSymbol))
        }
        refreshUI(Session.getAccount()!!.transferNotificationThreshold)
        system_notification.setOnClickListener {
            context?.openNotificationSetting()
        }
    }

    @SuppressLint("RestrictedApi")
    private fun showDialog(amount: String?) {
        if (context == null || !isAdded) {
            return
        }
        editDialog {
            titleText = this@NotificationsFragment.getString(R.string.setting_notification_transfer_amount, accountSymbol)
            editText = amount
            editHint = this@NotificationsFragment.getString(R.string.wallet_transfer_amount)
            editInputType = InputType.TYPE_NUMBER_FLAG_DECIMAL + InputType.TYPE_CLASS_NUMBER
            allowEmpty = false
            rightAction = {
                savePreference(it.toDouble())
            }
        }
    }

    private fun savePreference(threshold: Double) = lifecycleScope.launch {
        val pb = indeterminateProgressDialog(message = R.string.pb_dialog_message,
            title = R.string.setting_notification_transfer).apply {
            setCancelable(false)
        }
        pb.show()

        handleMixinResponse(
            invokeNetwork = {
                viewModel.preferences(AccountUpdateRequest(fiatCurrency = Session.getFiatCurrency(),
                    transferNotificationThreshold = threshold))
            },
            switchContext = Dispatchers.IO,
            successBlock = {
                it.data?.let { account ->
                    Session.storeAccount(account)
                    refreshUI(account.transferNotificationThreshold)
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
    private fun refreshUI(threshold: Double) {
        transfer_tv.text = "$threshold$accountSymbol"
        transfer_desc_tv.text = getString(R.string.setting_notification_transfer_desc,
            "$accountSymbol$threshold")
    }
}
