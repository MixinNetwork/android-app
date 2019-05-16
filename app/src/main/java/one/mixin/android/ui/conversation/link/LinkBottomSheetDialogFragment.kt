package one.mixin.android.ui.conversation.link

import android.annotation.SuppressLint
import android.app.Dialog
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.design.MixinBottomSheetDialogFragment
import android.view.Gravity
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import com.uber.autodispose.autoDisposable
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_bottom_sheet.view.*
import one.mixin.android.Constants.Scheme
import one.mixin.android.R
import one.mixin.android.api.request.TransferRequest
import one.mixin.android.api.response.AuthorizationResponse
import one.mixin.android.api.response.ConversationResponse
import one.mixin.android.api.response.PaymentStatus
import one.mixin.android.di.Injectable
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.isUUID
import one.mixin.android.extension.notNullElse
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.repository.QrCodeType
import one.mixin.android.ui.auth.AuthBottomSheetDialogFragment
import one.mixin.android.ui.common.BottomSheetViewModel
import one.mixin.android.ui.common.GroupBottomSheetDialogFragment
import one.mixin.android.ui.common.UserBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.TransferBiometricItem
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.conversation.tansfer.TransferBottomSheetDialogFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.Session
import one.mixin.android.vo.Asset
import one.mixin.android.vo.User
import one.mixin.android.vo.toAssetItem
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import javax.inject.Inject

class LinkBottomSheetDialogFragment : MixinBottomSheetDialogFragment(), Injectable {

    companion object {
        const val TAG = "LinkBottomSheetDialogFragment"
        const val CODE = "code"

        fun newInstance(code: String) = LinkBottomSheetDialogFragment().withArgs {
            putString(CODE, code)
        }
    }

    private val scopeProvider by lazy { AndroidLifecycleScopeProvider.from(this) }

    private var authOrPay = false

    override fun getTheme() = R.style.AppTheme_Dialog

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        if (Build.VERSION.SDK_INT >= 26) {
            dialog.window?.decorView?.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR or
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        contentView = View.inflate(context, R.layout.fragment_bottom_sheet, null)
        dialog.setContentView(contentView)
        val params = (contentView.parent as View).layoutParams as androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams
        val behavior = params.behavior

        if (behavior != null && behavior is BottomSheetBehavior<*>) {
            behavior.peekHeight = context!!.dpToPx(300f)
            behavior.setBottomSheetCallback(mBottomSheetBehaviorCallback)
            dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, context!!.dpToPx(300f))
            dialog.window?.setGravity(Gravity.BOTTOM)
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var code: String
    private lateinit var contentView: View
    private val linkViewModel: BottomSheetViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(BottomSheetViewModel::class.java)
    }

    private val url: String by lazy {
        arguments!!.getString(CODE)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (url.startsWith(Scheme.USERS, true) || url.startsWith(Scheme.HTTPS_USERS, true)) {
            val segments = Uri.parse(url).pathSegments
            val userId = if (segments.size >= 2) {
                segments[1]
            } else {
                segments[0]
            }
            if (!userId.isUUID()) {
                context?.toast(R.string.error_user_invalid_format)
                dismiss()
            } else {
                Flowable.just(userId).subscribeOn(Schedulers.io()).map {
                    var user = linkViewModel.getUserById(it)
                    if (user == null) {
                        val response = linkViewModel.getUser(it).execute()
                        if (response.isSuccessful) {
                            user = response.body()!!.data!!
                        }
                    }
                    user
                }.observeOn(AndroidSchedulers.mainThread()).autoDisposable(scopeProvider).subscribe({
                    notNullElse(it, {
                        dismiss()
                        UserBottomSheetDialogFragment.newInstance(it)
                            .showNow(requireFragmentManager(), UserBottomSheetDialogFragment.TAG)
                    }, {
                        context?.toast(R.string.error_user_not_found)
                        dismiss()
                    })
                }, {
                    context?.toast(R.string.error_user_not_found)
                    dismiss()
                })
            }
        } else if (url.startsWith(Scheme.HTTPS_PAY, true) || url.startsWith(Scheme.PAY, true)) {
            val uri = Uri.parse(url)
            val userId = uri.getQueryParameter("recipient")
            val assetId = uri.getQueryParameter("asset")
            val amount = uri.getQueryParameter("amount")
            val trace = uri.getQueryParameter("trace")
            val memo = uri.getQueryParameter("memo")
            if (userId == null || assetId == null || amount == null) {
                error(R.string.bottom_sheet_check_payment_info)
                return
            }
            val transferRequest = TransferRequest(assetId, userId, amount, null, trace, memo)
            linkViewModel.pay(transferRequest).autoDisposable(scopeProvider).subscribe({ r ->
                if (r.isSuccess) {
                    val paymentResponse = r.data!!
                    if (paymentResponse.status == PaymentStatus.paid.name) {
                        error(R.string.pay_paid)
                    } else {
                        authOrPay = true
                        showTransferBottom(paymentResponse.recipient, amount, paymentResponse.asset, trace, memo)
                        dismiss()
                    }
                } else {
                    ErrorHandler.handleMixinError(r.errorCode)
                    error(R.string.bottom_sheet_invalid_payment)
                }
            }, {
                error(R.string.bottom_sheet_check_payment_info)
                ErrorHandler.handleError(it)
            })
        } else if (url.startsWith(Scheme.HTTPS_CODES, true) || url.startsWith(Scheme.CODES, true)) {
            val segments = Uri.parse(url).pathSegments
            code = if (segments.size >= 2) {
                segments[1]
            } else {
                segments[0]
            }
            linkViewModel.searchCode(code).autoDisposable(scopeProvider).subscribe({ result ->
                when {
                    result.first == QrCodeType.conversation.name -> {
                        val response = result.second as ConversationResponse
                        val found = response.participants.find { it.userId == Session.getAccountId() }
                        if (found != null) {
                            linkViewModel.refreshConversation(response.conversationId)
                            context?.toast(R.string.group_already_in)
                            context?.let { ConversationActivity.show(it, response.conversationId) }
                        } else {
                            GroupBottomSheetDialogFragment.newInstance(response.conversationId, code)
                                .showNow(requireFragmentManager(), GroupBottomSheetDialogFragment.TAG)
                        }
                        dismiss()
                    }
                    result.first == QrCodeType.user.name -> {
                        val user = result.second as User
                        val account = Session.getAccount()
                        if (account != null && account.userId == (result.second as User).userId) {
                            context?.toast("It's your QR Code, please try another.")
                        } else {
                            UserBottomSheetDialogFragment.newInstance(user).showNow(requireFragmentManager(), UserBottomSheetDialogFragment.TAG)
                        }
                        dismiss()
                    }
                    result.first == QrCodeType.authorization.name -> {
                        val authorization = result.second as AuthorizationResponse
                        doAsync {
                            val assets = linkViewModel.simpleAssetsWithBalance()
                            uiThread {
                                activity?.let {
                                    val scopes = AuthBottomSheetDialogFragment
                                        .handleAuthorization(it, authorization, assets)
                                    AuthBottomSheetDialogFragment.newInstance(scopes, authorization)
                                        .showNow(requireFragmentManager(), AuthBottomSheetDialogFragment.TAG)
                                    authOrPay = true
                                    dismiss()
                                }
                            }
                        }
                    }
                    else -> error()
                }
            }, {
                error()
            })
        } else {
            error()
        }
    }

    override fun dismiss() {
        if (isAdded) {
            super.dismissAllowingStateLoss()
        }
    }

    private fun error(@StringRes errorRes: Int = R.string.group_error) {
        contentView.link_error_info.setText(errorRes)
        contentView.link_loading.visibility = GONE
        contentView.link_error_info.visibility = VISIBLE
    }

    private fun showTransferBottom(user: User, amount: String, asset: Asset, trace: String?, memo: String?) {
        TransferBottomSheetDialogFragment
            .newInstance(TransferBiometricItem(user, asset.toAssetItem(), amount, null, trace, memo))
            .showNow(requireFragmentManager(), TransferBottomSheetDialogFragment.TAG)
    }

    private val mBottomSheetBehaviorCallback = object : BottomSheetBehavior.BottomSheetCallback() {

        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                dismiss()
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {}
    }
}