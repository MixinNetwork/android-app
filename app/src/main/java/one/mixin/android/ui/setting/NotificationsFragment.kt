package one.mixin.android.ui.setting

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.fragment_notifications.*
import kotlinx.android.synthetic.main.view_title.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.AccountUpdateRequest
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.removeEnd
import one.mixin.android.extension.showKeyboard
import one.mixin.android.ui.common.BaseViewModelFragment
import one.mixin.android.util.Session
import one.mixin.android.vo.Fiats
import org.jetbrains.anko.dimen
import org.jetbrains.anko.margin

class NotificationsFragment : BaseViewModelFragment<SettingViewModel>() {
    companion object {
        const val TAG = "NotificationsFragment"
        fun newInstance(): NotificationsFragment {
            return NotificationsFragment()
        }
    }

    private val accountSymbol = Fiats.currencySymbol

    override fun getModelClass() = SettingViewModel::class.java

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_notifications, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }
        transfer_rl.setOnClickListener {
            showDialog(transfer_tv.text.toString().removeEnd(accountSymbol))
        }
        refreshUI(Session.getAccount()?.transferNotificationThreshold ?: "0")
    }

    @SuppressLint("RestrictedApi")
    private fun showDialog(amount: String?) {
        if (context == null || !isAdded) {
            return
        }
        val editText = EditText(requireContext())
        editText.hint = getString(R.string.wallet_transfer_amount)
        editText.inputType = InputType.TYPE_NUMBER_FLAG_DECIMAL + InputType.TYPE_CLASS_NUMBER
        editText.setText(amount)
        if (amount != null) {
            editText.setSelection(amount.length)
        }
        val frameLayout = FrameLayout(requireContext())
        frameLayout.addView(editText)
        val params = editText.layoutParams as FrameLayout.LayoutParams
        params.margin = context!!.dimen(R.dimen.activity_horizontal_margin)
        editText.layoutParams = params
        editText.setTextColor(requireContext().colorFromAttribute(R.attr.text_primary))
        val amountDialog = AlertDialog.Builder(context!!, R.style.MixinAlertDialogTheme)
            .setTitle(getString(R.string.setting_notification_transfer_amount, accountSymbol))
            .setView(frameLayout)
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton(R.string.confirm) { dialog, _ ->
                savePreference(editText.text.toString().toDouble())
                dialog.dismiss()
            }
            .show()
        amountDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = false
        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                amountDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = !(s.isNullOrBlank() || s.toString() == amount.toString())
            }
        })
        editText.post {
            editText.showKeyboard()
        }

        amountDialog.window?.clearFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        amountDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
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
    private fun refreshUI(threshold: String) {
        transfer_tv.text = "$threshold$accountSymbol"
        transfer_desc_tv.text = getString(R.string.setting_notification_transfer_desc,
            "${Fiats.currencySymbol}$threshold")
    }
}
