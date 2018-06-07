package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import com.uber.autodispose.kotlin.autoDisposable
import kotlinx.android.synthetic.main.fragment_get_free.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.Session
import one.mixin.android.vo.Account
import one.mixin.android.vo.toUser
import org.jetbrains.anko.dimen
import org.jetbrains.anko.margin
import javax.inject.Inject

class WalletGetFreeFragment : BaseFragment() {
    companion object {
        const val TAG = "WalletGetFreeFragment"

        fun newInstance(): WalletGetFreeFragment = WalletGetFreeFragment()
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val walletViewModel: WalletViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(WalletViewModel::class.java)
    }

    private var dialog: Dialog? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_get_free, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }
        val account = Session.getAccount()
        account?.let { renderInvitation(account) }
        redeem_rl.setOnClickListener { showDialog() }
    }

    @SuppressLint("RestrictedApi")
    private fun showDialog() {
        if (context == null) {
            return
        }
        val editText = EditText(context!!)
        editText.hint = getString(R.string.wallet_redeem)
        editText.inputType = InputType.TYPE_CLASS_NUMBER
        val frameLayout = FrameLayout(context)
        frameLayout.addView(editText)
        val params = editText.layoutParams as FrameLayout.LayoutParams
        params.margin = context!!.dimen(R.dimen.activity_horizontal_margin)
        editText.layoutParams = params
        dialog = android.support.v7.app.AlertDialog.Builder(context!!, R.style.MixinAlertDialogTheme)
            .setTitle(R.string.wallet_get_free_redeem)
            .setView(frameLayout)
            .setNegativeButton(R.string.cancel, { dialog, _ -> dialog.dismiss() })
            .setPositiveButton(R.string.wallet_redeem, { _, _ ->
                redeem(editText.text.toString())
            })
            .show()
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }

    private fun redeem(code: String) {
        if (code.isEmpty()) {
            context?.toast(R.string.can_not_empty)
            return
        }
        dialog?.dismiss()
        redeem_pb.visibility = VISIBLE
        walletViewModel.redeem(code).autoDisposable(scopeProvider).subscribe({ r: MixinResponse<Account> ->
            redeem_pb.visibility = GONE
            if (r.isSuccess) {
                r.data?.let {
                    Session.storeAccount(it)
                    walletViewModel.insertUser(it.toUser())
                    renderInvitation(it)
                }
            } else {
                ErrorHandler.handleMixinError(r.errorCode)
            }
        }, { t: Throwable ->
            redeem_pb.visibility = GONE
            ErrorHandler.handleError(t)
        })
    }

    private fun renderInvitation(account: Account) {
        if (account.invitation_code.isEmpty()) {
            invitation_rl.visibility = View.GONE
            redeem_rl.visibility = View.VISIBLE
            invitation_count_tv.text = getString(R.string.wallet_get_free_redeem_tip)
        } else {
            invitation_rl.visibility = View.VISIBLE
            redeem_rl.visibility = View.GONE
            invitation_desc_tv.text = account.invitation_code
            invitation_count_tv.text = getString(R.string.profile_invitation_tip, account.consumed_count)
        }
    }
}