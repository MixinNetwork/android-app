package one.mixin.android.ui.conversation.link

import android.annotation.SuppressLint
import android.app.Dialog
import android.net.Uri
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.StringRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.net.toUri
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.manager.SupportRequestManagerFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import one.mixin.android.Constants.Scheme
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.TransferRequest
import one.mixin.android.api.response.AuthorizationResponse
import one.mixin.android.api.response.ConversationResponse
import one.mixin.android.api.response.MultisigsResponse
import one.mixin.android.api.response.NonFungibleOutputResponse
import one.mixin.android.api.response.PaymentCodeResponse
import one.mixin.android.api.response.getScopes
import one.mixin.android.databinding.FragmentBottomSheetBinding
import one.mixin.android.extension.appendQueryParamsFromOtherUri
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.getGroupAvatarPath
import one.mixin.android.extension.handleSchemeSend
import one.mixin.android.extension.isDonateUrl
import one.mixin.android.extension.isExternalScheme
import one.mixin.android.extension.isUUID
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.job.getIconUrlName
import one.mixin.android.repository.QrCodeType
import one.mixin.android.session.Session
import one.mixin.android.ui.auth.AuthBottomSheetDialogFragment
import one.mixin.android.ui.common.BottomSheetViewModel
import one.mixin.android.ui.common.JoinGroupBottomSheetDialogFragment
import one.mixin.android.ui.common.JoinGroupConversation
import one.mixin.android.ui.common.MultisigsBottomSheetDialogFragment
import one.mixin.android.ui.common.NftBottomSheetDialogFragment
import one.mixin.android.ui.common.QrScanBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.AssetBiometricItem
import one.mixin.android.ui.common.biometric.Multi2MultiBiometricItem
import one.mixin.android.ui.common.biometric.NftBiometricItem
import one.mixin.android.ui.common.biometric.One2MultiBiometricItem
import one.mixin.android.ui.common.biometric.TransferBiometricItem
import one.mixin.android.ui.common.biometric.WithdrawBiometricItem
import one.mixin.android.ui.common.showUserBottom
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.conversation.PreconditionBottomSheetDialogFragment
import one.mixin.android.ui.conversation.PreconditionBottomSheetDialogFragment.Companion.FROM_LINK
import one.mixin.android.ui.conversation.TransferFragment
import one.mixin.android.ui.conversation.transfer.TransferBottomSheetDialogFragment
import one.mixin.android.ui.device.ConfirmBottomFragment
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.ui.url.UrlInterpreterActivity
import one.mixin.android.ui.wallet.PinAddrBottomSheetDialogFragment
import one.mixin.android.ui.wallet.TransactionBottomSheetDialogFragment
import one.mixin.android.ui.web.WebActivity
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.SystemUIManager
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Address
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.User
import one.mixin.android.vo.generateConversationId
import timber.log.Timber
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.UUID

@AndroidEntryPoint
class LinkBottomSheetDialogFragment : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "LinkBottomSheetDialogFragment"
        const val CODE = "code"

        fun newInstance(code: String) = LinkBottomSheetDialogFragment().withArgs {
            putString(CODE, code)
        }
    }

    private var authOrPay = false

    override fun getTheme() = R.style.AppTheme_Dialog

    private val linkViewModel by viewModels<BottomSheetViewModel>()

    private val binding by viewBinding(FragmentBottomSheetBinding::inflate)

    private lateinit var code: String
    private lateinit var contentView: View

    private val url: String by lazy { requireArguments().getString(CODE)!! }

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

    private fun getUserOrAppNotFoundTip(isApp: Boolean) = if (isApp) R.string.Bot_not_found else R.string.User_not_found

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        if (Build.VERSION.SDK_INT >= 26) {
            dialog.window?.decorView?.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR or
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        contentView = binding.root
        dialog.setContentView(contentView)
        val behavior = ((contentView.parent as View).layoutParams as? CoordinatorLayout.LayoutParams)?.behavior
        if (behavior != null && behavior is BottomSheetBehavior<*>) {
            behavior.peekHeight = requireContext().dpToPx(300f)
            behavior.addBottomSheetCallback(mBottomSheetBehaviorCallback)
            dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, requireContext().dpToPx(300f))
            dialog.window?.setGravity(Gravity.BOTTOM)
        }

        val isUserScheme = url.startsWith(Scheme.USERS, true) || url.startsWith(Scheme.HTTPS_USERS, true)
        val isAppScheme = url.startsWith(Scheme.APPS, true) || url.startsWith(Scheme.HTTPS_APPS, true)
        if (isUserScheme || isAppScheme) {
            val uri = url.toUri()
            val segments = uri.pathSegments
            if (segments.isEmpty()) return

            val userId = if (segments.size >= 2) {
                segments[1]
            } else {
                segments[0]
            }
            if (!userId.isUUID()) {
                toast(getUserOrAppNotFoundTip(isAppScheme))
                dismiss()
            } else {
                lifecycleScope.launch(errorHandler) {
                    val user = linkViewModel.refreshUser(userId)
                    if (user == null) {
                        toast(getUserOrAppNotFoundTip(isAppScheme))
                        dismiss()
                        return@launch
                    }
                    val isOpenApp = isAppScheme && uri.getQueryParameter("action") == "open"
                    if (isOpenApp && user.appId != null) {
                        lifecycleScope.launch(errorHandler) {
                            val app = linkViewModel.findAppById(user.appId!!)
                            if (app != null) {
                                val url = try {
                                    app.homeUri.appendQueryParamsFromOtherUri(uri)
                                } catch (e: Exception) {
                                    app.homeUri
                                }
                                WebActivity.show(requireActivity(), url, null, app)
                            } else {
                                showUserBottom(parentFragmentManager, user)
                            }
                        }
                    } else {
                        showUserBottom(parentFragmentManager, user)
                    }
                    dismiss()
                }
            }
        } else if (url.startsWith(Scheme.TRANSFER, true) || url.startsWith(Scheme.HTTPS_TRANSFER, true)) {
            if (checkHasPin()) return

            val uri = url.toUri()
            val segments = uri.pathSegments
            if (segments.isEmpty()) return

            val userId = if (segments.size >= 2) {
                segments[1]
            } else {
                segments[0]
            }
            if (!userId.isUUID()) {
                toast(R.string.User_not_found)
                dismiss()
            } else if (userId == Session.getAccountId()) {
                toast(R.string.cant_transfer_self)
                dismiss()
            } else {
                lifecycleScope.launch(errorHandler) {
                    val user = linkViewModel.refreshUser(userId)
                    if (user == null) {
                        toast(R.string.User_not_found)
                        dismiss()
                        return@launch
                    }
                    TransferFragment.newInstance(userId, supportSwitchAsset = true)
                        .showNow(parentFragmentManager, TransferFragment.TAG)
                    dismiss()
                }
            }
        } else if (url.startsWith(Scheme.HTTPS_PAY, true) ||
            url.startsWith(Scheme.PAY, true)
        ) {
            if (checkHasPin()) return

            lifecycleScope.launch(errorHandler) {
                if (!showTransfer(url)) {
                    showError(R.string.Invalid_payment_link)
                } else {
                    dismiss()
                }
            }
        } else if (url.startsWith(Scheme.HTTPS_CODES, true) || url.startsWith(Scheme.CODES, true)) {
            val segments = Uri.parse(url).pathSegments
            if (segments.isEmpty()) return

            code = if (segments.size >= 2) {
                segments[1]
            } else {
                segments[0]
            }
            lifecycleScope.launch(errorHandler) {
                val result = linkViewModel.searchCode(code)
                when (result.first) {
                    QrCodeType.conversation.name -> {
                        val response = result.second as ConversationResponse
                        val found =
                            response.participants.find { it.userId == Session.getAccountId() }
                        if (found != null) {
                            linkViewModel.refreshConversation(response.conversationId)
                            toast(R.string.group_already_in)
                            context?.let { ConversationActivity.show(it, response.conversationId) }
                            dismiss()
                        } else {
                            val avatarUserIds = mutableListOf<String>()
                            val notExistsUserIdList = mutableListOf<String>()
                            for (p in response.participants) {
                                val u = linkViewModel.suspendFindUserById(p.userId)
                                if (u == null) {
                                    notExistsUserIdList.add(p.userId)
                                }
                                if (avatarUserIds.size < 4) {
                                    avatarUserIds.add(p.userId)
                                }
                            }
                            val avatar4List = avatarUserIds.take(4)
                            val iconUrl = if (notExistsUserIdList.isNotEmpty()) {
                                linkViewModel.refreshUsers(
                                    notExistsUserIdList,
                                    response.conversationId,
                                    avatar4List
                                )
                                null
                            } else {
                                val avatarUsers =
                                    linkViewModel.findMultiUsersByIds(avatar4List.toSet())
                                linkViewModel.startGenerateAvatar(
                                    response.conversationId,
                                    avatar4List
                                )

                                val name = getIconUrlName(response.conversationId, avatarUsers)
                                val f = requireContext().getGroupAvatarPath(name, false)
                                f.absolutePath
                            }
                            val joinGroupConversation = JoinGroupConversation(
                                response.conversationId,
                                response.name,
                                response.announcement,
                                response.participants.size,
                                iconUrl
                            )
                            JoinGroupBottomSheetDialogFragment.newInstance(
                                joinGroupConversation,
                                code
                            ).showNow(
                                parentFragmentManager,
                                JoinGroupBottomSheetDialogFragment.TAG
                            )
                            dismiss()
                        }
                    }
                    QrCodeType.user.name -> {
                        val user = result.second as User
                        val account = Session.getAccount()
                        if (account != null && account.userId == (result.second as User).userId) {
                            toast("It's your QR Code, please try another.")
                        } else {
                            showUserBottom(parentFragmentManager, user)
                        }
                        dismiss()
                    }
                    QrCodeType.authorization.name -> {
                        val authorization = result.second as AuthorizationResponse
                        val assets = linkViewModel.simpleAssetsWithBalance()
                        activity?.let {
                            val scopes = authorization.getScopes(it, assets)
                            AuthBottomSheetDialogFragment.newInstance(scopes, authorization)
                                .showNow(
                                    parentFragmentManager,
                                    AuthBottomSheetDialogFragment.TAG
                                )
                            authOrPay = true
                            dismiss()
                        }
                    }
                    QrCodeType.multisig_request.name -> {
                        if (checkHasPin())return@launch

                        val multisigs = result.second as MultisigsResponse
                        val asset = checkAsset(multisigs.assetId)
                        if (asset != null) {
                            val multisigsBiometricItem = Multi2MultiBiometricItem(
                                requestId = multisigs.requestId,
                                action = multisigs.action,
                                senders = multisigs.senders,
                                receivers = multisigs.receivers,
                                threshold = multisigs.threshold,
                                asset = asset,
                                amount = multisigs.amount,
                                pin = null,
                                traceId = null,
                                memo = multisigs.memo,
                                state = multisigs.state
                            )
                            MultisigsBottomSheetDialogFragment.newInstance(
                                multisigsBiometricItem
                            )
                                .showNow(
                                    parentFragmentManager,
                                    MultisigsBottomSheetDialogFragment.TAG
                                )
                            dismiss()
                        } else {
                            showError()
                        }
                    }
                    QrCodeType.non_fungible_request.name -> {
                        if (checkHasPin()) return@launch
                        val nfoResponse = result.second as NonFungibleOutputResponse
                        val nftBiometricItem = NftBiometricItem(
                            requestId = nfoResponse.requestId,
                            action = nfoResponse.action,
                            tokenId = nfoResponse.tokenId,
                            senders = nfoResponse.senders,
                            receivers = nfoResponse.receivers,
                            sendersThreshold = nfoResponse.sendersThreshold,
                            receiversThreshold = nfoResponse.receiversThreshold,
                            rawTransaction = nfoResponse.rawTransaction,
                            amount = nfoResponse.amount,
                            pin = null,
                            memo = nfoResponse.memo,
                            state = nfoResponse.state
                        )
                        NftBottomSheetDialogFragment.newInstance(nftBiometricItem)
                            .showNow(parentFragmentManager, NftBottomSheetDialogFragment.TAG)
                        dismiss()
                    }
                    QrCodeType.payment.name -> {
                        if (checkHasPin()) return@launch

                        val paymentCodeResponse = result.second as PaymentCodeResponse
                        val asset = checkAsset(paymentCodeResponse.assetId)
                        if (asset != null) {
                            val multisigsBiometricItem = One2MultiBiometricItem(
                                threshold = paymentCodeResponse.threshold,
                                senders = arrayOf(Session.getAccountId()!!),
                                receivers = paymentCodeResponse.receivers,
                                asset = asset,
                                amount = paymentCodeResponse.amount,
                                pin = null,
                                traceId = paymentCodeResponse.traceId,
                                memo = paymentCodeResponse.memo,
                                state = paymentCodeResponse.status
                            )
                            MultisigsBottomSheetDialogFragment.newInstance(
                                multisigsBiometricItem
                            )
                                .showNow(
                                    parentFragmentManager,
                                    MultisigsBottomSheetDialogFragment.TAG
                                )
                            dismiss()
                        } else {
                            showError()
                        }
                    }
                    else -> showError()
                }
            }
        } else if (url.startsWith(Scheme.HTTPS_ADDRESS, true) || url.startsWith(Scheme.ADDRESS, true)) {
            if (checkHasPin()) return

            val uri = Uri.parse(url)
            val action = uri.getQueryParameter("action")
            if (action != null && action == "delete") {
                val assetId = uri.getQueryParameter("asset")
                val addressId = uri.getQueryParameter("address")
                if (assetId != null && assetId.isUUID() && addressId != null && addressId.isUUID()) {
                    lifecycleScope.launch(errorHandler) {
                        val pair = linkViewModel.findAddressById(addressId, assetId)
                        val address = pair.first
                        if (pair.second) {
                            showError(R.string.error_address_exists)
                        } else if (address == null) {
                            showError(R.string.error_address_not_sync)
                        } else {
                            val asset = checkAsset(assetId)
                            if (asset != null) {
                                PinAddrBottomSheetDialogFragment.newInstance(
                                    assetId = assetId,
                                    assetUrl = asset.iconUrl,
                                    assetSymbol = asset.symbol,
                                    chainId = asset.chainId,
                                    chainName = asset.chainName,
                                    chainIconUrl = asset.chainIconUrl,
                                    assetName = asset.name,
                                    addressId = addressId,
                                    label = address.label,
                                    destination = address.destination,
                                    tag = address.tag,
                                    type = PinAddrBottomSheetDialogFragment.DELETE
                                ).showNow(this@LinkBottomSheetDialogFragment.parentFragmentManager, PinAddrBottomSheetDialogFragment.TAG)
                                dismiss()
                            } else {
                                showError()
                            }
                        }
                    }
                } else {
                    showError()
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
                    lifecycleScope.launch(errorHandler) {
                        val asset = checkAsset(assetId)
                        if (asset != null) {
                            PinAddrBottomSheetDialogFragment.newInstance(
                                assetId = assetId,
                                assetUrl = asset.iconUrl,
                                assetSymbol = asset.symbol,
                                chainId = asset.chainId,
                                chainName = asset.chainName,
                                chainIconUrl = asset.chainIconUrl,
                                assetName = asset.name,
                                label = label,
                                destination = destination,
                                tag = tag,
                                type = PinAddrBottomSheetDialogFragment.ADD
                            )
                                .showNow(this@LinkBottomSheetDialogFragment.parentFragmentManager, PinAddrBottomSheetDialogFragment.TAG)
                            dismiss()
                        } else {
                            showError()
                        }
                    }
                } else {
                    showError()
                }
            }
        } else if (url.startsWith(Scheme.SNAPSHOTS, true)) {
            if (checkHasPin()) return

            val uri = Uri.parse(url)
            val traceId = uri.getQueryParameter("trace")
            if (!traceId.isNullOrEmpty() && traceId.isUUID()) {
                lifecycleScope.launch(errorHandler) {
                    val result = linkViewModel.getSnapshotByTraceId(traceId)
                    if (result != null) {
                        dismiss()
                        TransactionBottomSheetDialogFragment.newInstance(result.first, result.second)
                            .show(parentFragmentManager, TransactionBottomSheetDialogFragment.TAG)
                    } else {
                        showError()
                    }
                }
                return
            }
            val snapshotId = uri.lastPathSegment
            if (snapshotId.isNullOrEmpty() || !snapshotId.isUUID()) {
                showError()
            } else {
                lifecycleScope.launch(errorHandler) {
                    val result = linkViewModel.getSnapshotAndAsset(snapshotId)
                    if (result != null) {
                        dismiss()
                        TransactionBottomSheetDialogFragment.newInstance(result.first, result.second)
                            .show(parentFragmentManager, TransactionBottomSheetDialogFragment.TAG)
                    } else {
                        showError()
                    }
                }
            }
        } else if (url.startsWith(Scheme.HTTPS_WITHDRAWAL, true) || url.startsWith(Scheme.WITHDRAWAL, true)) {
            if (checkHasPin()) return

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
                showError()
            } else {
                lifecycleScope.launch(errorHandler) {
                    val pair = linkViewModel.findAddressById(addressId, assetId)
                    val address = pair.first
                    val asset = checkAsset(assetId)
                    if (asset != null) {
                        when {
                            pair.second -> {
                                showError(R.string.error_address_exists)
                            }
                            address == null -> {
                                showError(R.string.error_address_not_sync)
                            }
                            else -> {
                                val dust = address.dust?.toDoubleOrNull()
                                val amountDouble = amount.toDoubleOrNull()
                                if (dust != null && amountDouble != null && amountDouble < dust) {
                                    val errorString = getString(R.string.withdrawal_minimum_amount, address.dust, asset.symbol)
                                    showError(errorString)
                                    toast(errorString)
                                    return@launch
                                }

                                val transferRequest = TransferRequest(assetId, null, amount, null, traceId, memo, addressId)
                                handleMixinResponse(
                                    invokeNetwork = {
                                        linkViewModel.paySuspend(transferRequest)
                                    },
                                    successBlock = { r ->
                                        val response = r.data ?: return@handleMixinResponse false

                                        showWithdrawalBottom(address, amount, asset, traceId, response.status, memo)
                                    },
                                    failureBlock = {
                                        showError(R.string.Invalid_payment_link)
                                        return@handleMixinResponse false
                                    },
                                    exceptionBlock = {
                                        showError(R.string.Checking_payment_info)
                                        return@handleMixinResponse false
                                    }
                                )
                            }
                        }
                    } else {
                        showError(R.string.Asset_not_found)
                    }
                }
            }
        } else if (url.isDonateUrl()) {
            if (checkHasPin()) return

            lifecycleScope.launch(errorHandler) {
                val newUrl = url.replaceFirst(":", "://")
                if (!showTransfer(newUrl)) {
                    QrScanBottomSheetDialogFragment.newInstance(url)
                        .show(parentFragmentManager, QrScanBottomSheetDialogFragment.TAG)
                }
                dismiss()
            }
        } else if (url.startsWith(Scheme.CONVERSATIONS, true)) {
            val uri = Uri.parse(url)
            val segments = uri.pathSegments
            if (segments.isEmpty()) return

            val conversationId = segments[0]
            if (conversationId.isEmpty() || !conversationId.isUUID()) {
                showError()
                return
            }
            val userId = uri.getQueryParameter("user")
            lifecycleScope.launch(errorHandler) {
                if (userId != null) {
                    val user = linkViewModel.refreshUser(userId)
                    when {
                        user == null -> {
                            showError(R.string.User_not_found)
                        }
                        conversationId != generateConversationId(requireNotNull(Session.getAccountId()), userId) -> {
                            showError()
                        }
                        else -> {
                            ConversationActivity.show(requireContext(), conversationId, userId)
                            dismiss()
                        }
                    }
                } else {
                    val conversation = linkViewModel.getAndSyncConversation(conversationId)
                    if (conversation != null) {
                        ConversationActivity.show(requireContext(), conversation.conversationId)
                        dismiss()
                    } else {
                        showError(R.string.Conversation_not_found)
                    }
                }
            }
        } else if (url.startsWith(Scheme.SEND, true)) {
            val uri = Uri.parse(url)
            uri.handleSchemeSend(
                requireContext(),
                parentFragmentManager,
                showNow = false,
                afterShareText = { dismiss() },
                afterShareData = { dismiss() },
                onError = { err ->
                    showError(err)
                }
            )
        } else if (url.startsWith(Scheme.DEVICE, true)) {
            contentView.post {
                ConfirmBottomFragment.show(requireContext(), parentFragmentManager, url)
                dismiss()
            }
        } else {
            if (url.isExternalScheme(requireContext())) {
                WebActivity.show(requireContext(), url, null)
                dismiss()
            } else {
                showError()
            }
        }
    }

    private fun checkHasPin(): Boolean {
        if (Session.getAccount()?.hasPin == false) {
            MainActivity.showWallet(requireContext())
            dismiss()
            return true
        }
        return false
    }

    override fun dismiss() {
        if (isAdded) {
            try {
                super.dismiss()
            } catch (e: IllegalStateException) {
                Timber.w(e)
            }
        }
    }

    override fun showNow(manager: FragmentManager, tag: String?) {
        try {
            super.showNow(manager, tag)
        } catch (e: IllegalStateException) {
            Timber.w(e)
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

    private suspend fun showTransfer(text: String): Boolean {
        val uri = text.toUri()
        val amount = uri.getQueryParameter("amount") ?: return false
        if (amount.toDoubleOrNull() == null) return false
        val userId = uri.getQueryParameter("recipient")
        if (userId == null || !userId.isUUID()) {
            return false
        }
        val assetId = uri.getQueryParameter("asset")
        if (assetId == null || !assetId.isUUID()) {
            return false
        }
        val trace = uri.getQueryParameter("trace") ?: UUID.randomUUID().toString()
        val memo = uri.getQueryParameter("memo")

        val asset: AssetItem = checkAsset(assetId) ?: return false

        val user = linkViewModel.refreshUser(userId) ?: return false

        val transferRequest = TransferRequest(assetId, userId, amount, null, trace, memo)
        return handleMixinResponse(
            invokeNetwork = {
                linkViewModel.paySuspend(transferRequest)
            },
            successBlock = { r ->
                val response = r.data ?: return@handleMixinResponse false

                showTransferBottom(user, amount, asset, trace, response.status, memo)
                return@handleMixinResponse true
            }
        ) ?: false
    }

    private suspend fun showTransferBottom(user: User, amount: String, asset: AssetItem, traceId: String, status: String, memo: String?) {
        val pair = linkViewModel.findLatestTrace(user.userId, null, null, amount, asset.assetId)
        if (pair.second) {
            showError(getString(R.string.check_trace_failed))
            return
        }
        val biometricItem = TransferBiometricItem(user, asset, amount, null, traceId, memo, status, pair.first)
        showPreconditionBottom(biometricItem)
    }

    private suspend fun showWithdrawalBottom(address: Address, amount: String, asset: AssetItem, traceId: String, status: String, memo: String?) {
        val pair = linkViewModel.findLatestTrace(null, address.destination, address.tag, amount, asset.assetId)
        if (pair.second) {
            showError(getString(R.string.check_trace_failed))
            return
        }
        val biometricItem = WithdrawBiometricItem(
            address.destination, address.tag, address.addressId, address.label, address.fee,
            asset, amount, null, traceId, memo, status, pair.first
        )
        showPreconditionBottom(biometricItem)
    }

    private fun showPreconditionBottom(biometricItem: AssetBiometricItem) {
        val preconditionBottom = PreconditionBottomSheetDialogFragment.newInstance(biometricItem, FROM_LINK)
        preconditionBottom.callback = object : PreconditionBottomSheetDialogFragment.Callback {
            override fun onSuccess() {
                val bottom = TransferBottomSheetDialogFragment.newInstance(biometricItem)
                bottom.show(preconditionBottom.parentFragmentManager, TransferBottomSheetDialogFragment.TAG)
                dismiss()
            }

            override fun onCancel() {
                dismiss()
            }
        }
        preconditionBottom.showNow(parentFragmentManager, PreconditionBottomSheetDialogFragment.TAG)
    }

    private suspend fun checkAsset(assetId: String): AssetItem? {
        var asset = linkViewModel.findAssetItemById(assetId)
        if (asset == null) {
            asset = linkViewModel.refreshAsset(assetId)
        }
        if (asset != null && asset.assetId != asset.chainId && linkViewModel.findAssetItemById(asset.chainId) == null) {
            linkViewModel.refreshAsset(asset.chainId)
        }
        return linkViewModel.findAssetItemById(assetId)
    }

    @SuppressLint("SetTextI18n")
    private fun showError(@StringRes errorRes: Int = R.string.Invalid_Link) {
        binding.apply {
            if (errorRes == R.string.Invalid_Link) {
                linkErrorInfo.text = "${getString(R.string.Invalid_Link)}\n\n$url"
            } else {
                linkErrorInfo.setText(errorRes)
            }
            linkLoading.visibility = GONE
            linkErrorInfo.visibility = VISIBLE
            linkErrorInfo.setTextIsSelectable(true)
        }
    }

    private fun showError(error: String) {
        binding.apply {
            linkErrorInfo.text = error
            linkLoading.visibility = GONE
            linkErrorInfo.visibility = VISIBLE
        }
    }

    private val mBottomSheetBehaviorCallback = object : BottomSheetBehavior.BottomSheetCallback() {

        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                try {
                    dismissAllowingStateLoss()
                } catch (e: IllegalStateException) {
                    Timber.w(e)
                }
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {}
    }

    private val errorHandler = CoroutineExceptionHandler { _, error ->
        when (error) {
            is SocketTimeoutException -> showError(R.string.error_connection_timeout)
            is UnknownHostException -> showError(R.string.No_network_connection)
            is IOException -> showError(R.string.No_network_connection)
            else -> {
                ErrorHandler.handleError(error)
                showError(R.string.Network_error)
            }
        }
    }
}
