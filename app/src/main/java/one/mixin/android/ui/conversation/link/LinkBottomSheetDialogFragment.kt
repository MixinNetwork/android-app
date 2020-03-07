package one.mixin.android.ui.conversation.link

import android.annotation.SuppressLint
import android.app.Dialog
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.StringRes
import androidx.core.net.toUri
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.manager.SupportRequestManagerFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import com.uber.autodispose.autoDispose
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_bottom_sheet.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants.Scheme
import one.mixin.android.R
import one.mixin.android.api.request.TransferRequest
import one.mixin.android.api.response.AuthorizationResponse
import one.mixin.android.api.response.ConversationResponse
import one.mixin.android.api.response.MultisigsResponse
import one.mixin.android.api.response.PaymentCodeResponse
import one.mixin.android.api.response.getScopes
import one.mixin.android.di.Injectable
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.isUUID
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.repository.QrCodeType
import one.mixin.android.ui.auth.AuthBottomSheetDialogFragment
import one.mixin.android.ui.common.BottomSheetViewModel
import one.mixin.android.ui.common.GroupBottomSheetDialogFragment
import one.mixin.android.ui.common.MultisigsBottomSheetDialogFragment
import one.mixin.android.ui.common.UserBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.Multi2MultiBiometricItem
import one.mixin.android.ui.common.biometric.One2MultiBiometricItem
import one.mixin.android.ui.common.biometric.TransferBiometricItem
import one.mixin.android.ui.common.biometric.WithdrawBiometricItem
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.conversation.tansfer.TransferBottomSheetDialogFragment
import one.mixin.android.ui.conversation.web.WebBottomSheetDialogFragment
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.ui.url.UrlInterpreterActivity
import one.mixin.android.ui.wallet.PinAddrBottomSheetDialogFragment
import one.mixin.android.ui.wallet.TransactionFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.Session
import one.mixin.android.util.SystemUIManager
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.User

class LinkBottomSheetDialogFragment : BottomSheetDialogFragment(), Injectable {

    companion object {
        const val TAG = "LinkBottomSheetDialogFragment"
        const val CODE = "code"

        fun newInstance(code: String) = LinkBottomSheetDialogFragment().withArgs {
            putString(CODE, code)
        }
    }

    private val scopeProvider by lazy { AndroidLifecycleScopeProvider.from(this, Lifecycle.Event.ON_STOP) }

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
            behavior.peekHeight = requireContext().dpToPx(300f)
            behavior.addBottomSheetCallback(mBottomSheetBehaviorCallback)
            dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, requireContext().dpToPx(300f))
            dialog.window?.setGravity(Gravity.BOTTOM)
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val linkViewModel: BottomSheetViewModel by viewModels { viewModelFactory }

    private lateinit var code: String
    private lateinit var contentView: View

    private val url: String by lazy { arguments!!.getString(CODE)!! }

    override fun onStart() {
        try {
            super.onStart()
        } catch (ignored: WindowManager.BadTokenException) {
        }
        dialog?.window?.let { window ->
            SystemUIManager.lightUI(
                window,
                !requireContext().booleanFromAttribute(R.attr.flag_night)
            )
        }
    }

    private fun getUserOrAppNotFoundTip(isApp: Boolean) = if (isApp) R.string.error_app_not_found else R.string.error_user_not_found

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val isUserScheme = url.startsWith(Scheme.USERS, true) || url.startsWith(Scheme.HTTPS_USERS, true)
        val isAppScheme = url.startsWith(Scheme.APPS, true) || url.startsWith(Scheme.HTTPS_APPS, true)
        if (isUserScheme || isAppScheme) {
            val uri = url.toUri()
            val segments = uri.pathSegments
            val userId = if (segments.size >= 2) {
                segments[1]
            } else {
                segments[0]
            }
            if (!userId.isUUID()) {
                context?.toast(getUserOrAppNotFoundTip(isAppScheme))
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
                }.observeOn(AndroidSchedulers.mainThread()).autoDispose(scopeProvider).subscribe({
                    it.notNullWithElse({ u ->
                        val isOpenApp = isAppScheme && uri.getQueryParameter("action") == "open"
                        if (isOpenApp && u.appId != null) {
                            lifecycleScope.launch {
                                val app = linkViewModel.findAppById(u.appId!!)
                                if (app != null) {
                                    WebBottomSheetDialogFragment.newInstance(app.homeUri, null, app)
                                        .showNow(parentFragmentManager, WebBottomSheetDialogFragment.TAG)
                                } else {
                                    UserBottomSheetDialogFragment.newInstance(u)
                                        .showNow(parentFragmentManager, UserBottomSheetDialogFragment.TAG)
                                }
                            }
                        } else {
                            UserBottomSheetDialogFragment.newInstance(u)
                                .showNow(parentFragmentManager, UserBottomSheetDialogFragment.TAG)
                        }
                        dismiss()
                    }, {
                        context?.toast(getUserOrAppNotFoundTip(isAppScheme))
                        dismiss()
                    })
                }, {
                    context?.toast(getUserOrAppNotFoundTip(isAppScheme))
                    dismiss()
                })
            }
        } else if (url.startsWith(Scheme.HTTPS_PAY, true) || url.startsWith(Scheme.PAY, true)) {
            if (Session.getAccount()?.hasPin == false) {
                MainActivity.showWallet(requireContext())
                dismiss()
                return
            }
            val uri = Uri.parse(url)
            val userId = uri.getQueryParameter("recipient")
            val assetId = uri.getQueryParameter("asset")
            val amount = uri.getQueryParameter("amount")
            val trace = uri.getQueryParameter("trace")
            val memo = uri.getQueryParameter("memo")
            if (userId == null || assetId == null || amount == null) {
                error(R.string.link_error)
                return
            }
            val transferRequest = TransferRequest(assetId, userId, amount, null, trace, memo)
            linkViewModel.pay(transferRequest).autoDispose(scopeProvider).subscribe({ r ->
                if (r.isSuccess) {
                    lifecycleScope.launch {
                        var asset = linkViewModel.findAssetItemById(assetId)
                        if (asset == null) {
                            asset = linkViewModel.refreshAsset(assetId)
                        }
                        val paymentResponse = r.data!!
                        if (asset != null) {
                            authOrPay = true
                            showTransferBottom(paymentResponse.recipient, amount, asset, trace, memo, paymentResponse.status)
                            dismiss()
                        } else {
                            error()
                        }
                    }
                } else {
                    ErrorHandler.handleMixinError(r.errorCode, r.errorDescription)
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
            linkViewModel.searchCode(code).autoDispose(scopeProvider).subscribe({ result ->
                when (result.first) {
                    QrCodeType.conversation.name -> {
                        val response = result.second as ConversationResponse
                        val found = response.participants.find { it.userId == Session.getAccountId() }
                        if (found != null) {
                            linkViewModel.refreshConversation(response.conversationId)
                            context?.toast(R.string.group_already_in)
                            context?.let { ConversationActivity.show(it, response.conversationId) }
                        } else {
                            GroupBottomSheetDialogFragment.newInstance(response.conversationId, code)
                                .showNow(parentFragmentManager, GroupBottomSheetDialogFragment.TAG)
                        }
                        dismiss()
                    }
                    QrCodeType.user.name -> {
                        val user = result.second as User
                        val account = Session.getAccount()
                        if (account != null && account.userId == (result.second as User).userId) {
                            context?.toast("It's your QR Code, please try another.")
                        } else {
                            UserBottomSheetDialogFragment.newInstance(user).showNow(parentFragmentManager, UserBottomSheetDialogFragment.TAG)
                        }
                        dismiss()
                    }
                    QrCodeType.authorization.name -> {
                        val authorization = result.second as AuthorizationResponse
                        lifecycleScope.launch {
                            val assets = withContext(Dispatchers.IO) {
                                linkViewModel.simpleAssetsWithBalance()
                            }
                            activity?.let {
                                val scopes = authorization.getScopes(it, assets)
                                AuthBottomSheetDialogFragment.newInstance(scopes, authorization)
                                    .showNow(parentFragmentManager, AuthBottomSheetDialogFragment.TAG)
                                authOrPay = true
                                dismiss()
                            }
                        }
                    }
                    QrCodeType.multisig_request.name -> {
                        val multisigs = result.second as MultisigsResponse
                        lifecycleScope.launch {
                            var asset = linkViewModel.findAssetItemById(multisigs.assetId)
                            if (asset == null) {
                                asset = linkViewModel.refreshAsset(multisigs.assetId)
                            }
                            if (asset != null) {
                                val multisigsBiometricItem = Multi2MultiBiometricItem(
                                    requestId = multisigs.requestId,
                                    action = multisigs.action,
                                    senders = multisigs.senders,
                                    receivers = multisigs.receivers,
                                    asset = asset!!,
                                    amount = multisigs.amount,
                                    pin = null,
                                    trace = null,
                                    memo = multisigs.memo,
                                    state = multisigs.state
                                )
                                MultisigsBottomSheetDialogFragment.newInstance(multisigsBiometricItem)
                                    .showNow(parentFragmentManager, MultisigsBottomSheetDialogFragment.TAG)
                                dismiss()
                            } else {
                                error()
                            }
                        }
                    }
                    QrCodeType.payment.name -> {
                        val paymentCodeResponse = result.second as PaymentCodeResponse
                        lifecycleScope.launch {
                            var asset = linkViewModel.findAssetItemById(paymentCodeResponse.assetId)
                            if (asset == null) {
                                asset = linkViewModel.refreshAsset(paymentCodeResponse.assetId)
                            }
                            if (asset != null) {
                                val multisigsBiometricItem = One2MultiBiometricItem(
                                    threshold = paymentCodeResponse.threshold,
                                    senders = arrayOf(Session.getAccountId()!!),
                                    receivers = paymentCodeResponse.receivers,
                                    asset = asset!!,
                                    amount = paymentCodeResponse.amount,
                                    pin = null,
                                    trace = paymentCodeResponse.traceId,
                                    memo = paymentCodeResponse.memo,
                                    state = paymentCodeResponse.status
                                )
                                MultisigsBottomSheetDialogFragment.newInstance(multisigsBiometricItem)
                                    .showNow(parentFragmentManager, MultisigsBottomSheetDialogFragment.TAG)
                                dismiss()
                            } else {
                                error()
                            }
                        }
                    }
                    else -> error()
                }
            }, {
                error()
            })
        } else if (url.startsWith(Scheme.HTTPS_ADDRESS, true) || url.startsWith(Scheme.ADDRESS, true)) {
            if (Session.getAccount()?.hasPin == false) {
                MainActivity.showWallet(requireContext())
                dismiss()
                return
            }
            val uri = Uri.parse(url)
            val action = uri.getQueryParameter("action")
            if (action != null && action == "delete") {
                val assetId = uri.getQueryParameter("asset")
                val addressId = uri.getQueryParameter("address")
                if (assetId != null && assetId.isUUID() && addressId != null && addressId.isUUID()) {
                    lifecycleScope.launch {
                        val address = linkViewModel.findAddressById(addressId, assetId)
                        if (address == null) {
                            error(R.string.error_address_exists)
                        } else {
                            var asset = linkViewModel.findAssetItemById(assetId)
                            if (asset == null) {
                                asset = linkViewModel.refreshAsset(assetId)
                            }
                            if (asset != null) {
                                PinAddrBottomSheetDialogFragment.newInstance(
                                    assetId = assetId,
                                    assetUrl = asset!!.iconUrl,
                                    chainIconUrl = asset!!.chainIconUrl,
                                    assetName = asset!!.name,
                                    addressId = addressId,
                                    label = address.label,
                                    destination = address.destination,
                                    tag = address.tag,
                                    type = PinAddrBottomSheetDialogFragment.DELETE
                                ).showNow(this@LinkBottomSheetDialogFragment.parentFragmentManager, PinAddrBottomSheetDialogFragment.TAG)
                                dismiss()
                            } else {
                                error()
                            }
                        }
                    }
                } else {
                    error()
                }
            } else {
                val assetId = uri.getQueryParameter("asset")
                val destination = uri.getQueryParameter("destination")
                val label = uri.getQueryParameter("label").run {
                    Uri.decode(this)
                }
                val tag = uri.getQueryParameter("tag").run {
                    Uri.decode(this)
                }
                if (assetId != null && assetId.isUUID() && !destination.isNullOrEmpty() && !label.isNullOrEmpty()) {
                    lifecycleScope.launch {
                        var asset = linkViewModel.findAssetItemById(assetId)
                        if (asset == null) {
                            asset = linkViewModel.refreshAsset(assetId)
                        }
                        if (asset != null) {
                            PinAddrBottomSheetDialogFragment.newInstance(
                                assetId = assetId,
                                assetUrl = asset!!.iconUrl,
                                chainIconUrl = asset!!.chainIconUrl,
                                assetName = asset!!.name,
                                label = label,
                                destination = destination,
                                tag = tag,
                                type = PinAddrBottomSheetDialogFragment.ADD
                            )
                                .showNow(this@LinkBottomSheetDialogFragment.parentFragmentManager, PinAddrBottomSheetDialogFragment.TAG)
                            dismiss()
                        } else {
                            error()
                        }
                    }
                } else {
                    error()
                }
            }
        } else if (url.startsWith(Scheme.SNAPSHOTS, true)) {
            if (Session.getAccount()?.hasPin == false) {
                MainActivity.showWallet(requireContext())
                dismiss()
                return
            }
            val uri = Uri.parse(url)
            val traceId = uri.getQueryParameter("trace")
            if (!traceId.isNullOrEmpty() && traceId.isUUID()) {
                lifecycleScope.launch {
                    val result = linkViewModel.getSnapshotByTraceId(traceId)
                    if (result != null) {
                        dismiss()
                        activity?.addFragment(
                            this@LinkBottomSheetDialogFragment,
                            TransactionFragment.newInstance(result.first, result.second),
                            TransactionFragment.TAG
                        )
                    } else {
                        error()
                    }
                }
                return
            }
            val snapshotId = uri.lastPathSegment
            if (snapshotId.isNullOrEmpty() || !snapshotId.isUUID()) {
                error()
            } else {
                lifecycleScope.launch {
                    val result = linkViewModel.getSnapshotAndAsset(snapshotId)
                    if (result != null) {
                        dismiss()
                        activity?.addFragment(
                            this@LinkBottomSheetDialogFragment,
                            TransactionFragment.newInstance(result.first, result.second),
                            TransactionFragment.TAG
                        )
                    } else {
                        error()
                    }
                }
            }
        } else if (url.startsWith(Scheme.HTTPS_WITHDRAWAL, true) || url.startsWith(Scheme.WITHDRAWAL, true)) {
            if (Session.getAccount()?.hasPin == false) {
                MainActivity.showWallet(requireContext())
                dismiss()
                return
            }
            val uri = Uri.parse(url)

            val assetId = uri.getQueryParameter("asset")
            val amount = uri.getQueryParameter("amount")
            val memo = uri.getQueryParameter("memo")?.run {
                Uri.decode(this)
            }
            val traceId = uri.getQueryParameter("trace")
            val addressId = uri.getQueryParameter("address")
            if (assetId.isNullOrEmpty() || addressId.isNullOrEmpty() ||
                amount.isNullOrEmpty() || traceId.isNullOrEmpty() || !assetId.isUUID() ||
                !traceId.isUUID()
            ) {
                error()
            } else {
                val transferRequest = TransferRequest(assetId, null, amount, null, traceId, memo, addressId)
                linkViewModel.pay(transferRequest).autoDispose(scopeProvider).subscribe({ r ->
                    if (r.isSuccess) {
                        val paymentResponse = r.data!!
                        lifecycleScope.launch {
                            val address = linkViewModel.findAddressById(addressId, assetId)
                            var asset = linkViewModel.findAssetItemById(assetId)
                            if (asset == null) {
                                asset = linkViewModel.refreshAsset(assetId)
                            }
                            if (asset != null) {
                                when {
                                    address == null -> error(R.string.error_address_exists)
                                    asset == null -> error(R.string.error_asset_exists)
                                    else -> {
                                        val biometricItem =
                                            WithdrawBiometricItem(
                                                address.destination, address.addressId,
                                                address.label, asset!!, amount, null, traceId, memo, paymentResponse.status
                                            )
                                        val bottom = TransferBottomSheetDialogFragment.newInstance(biometricItem)
                                        bottom.showNow(parentFragmentManager, TransferBottomSheetDialogFragment.TAG)
                                        dismiss()
                                    }
                                }
                            } else {
                                error()
                            }
                        }
                    } else {
                        ErrorHandler.handleMixinError(r.errorCode, r.errorDescription)
                        error(R.string.bottom_sheet_invalid_payment)
                    }
                }, {
                    error(R.string.bottom_sheet_check_payment_info)
                    ErrorHandler.handleError(it)
                })
            }
        } else {
            error()
        }
    }

    override fun dismiss() {
        if (isAdded) {
            try {
                super.dismiss()
            } catch (e: IllegalStateException) {
            }
        }
    }

    override fun showNow(manager: FragmentManager, tag: String?) {
        try {
            super.showNow(manager, tag)
        } catch (e: IllegalStateException) {
        }
    }

    override fun onDetach() {
        super.onDetach()
        if (activity is UrlInterpreterActivity) {
            var realFragmentCount = 0
            parentFragmentManager.fragments.forEach { f ->
                if (f !is SupportRequestManagerFragment) {
                    realFragmentCount++
                }
            }
            if (realFragmentCount <= 0) {
                activity?.finish()
            }
        }
    }

    private fun error(@StringRes errorRes: Int = R.string.link_error) {
        contentView.link_error_info.setText(errorRes)
        contentView.link_loading.visibility = GONE
        contentView.link_error_info.visibility = VISIBLE
    }

    private fun showTransferBottom(user: User, amount: String, asset: AssetItem, trace: String?, memo: String?, state: String) {
        TransferBottomSheetDialogFragment
            .newInstance(TransferBiometricItem(user, asset, amount, null, trace, memo, state))
            .showNow(parentFragmentManager, TransferBottomSheetDialogFragment.TAG)
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
