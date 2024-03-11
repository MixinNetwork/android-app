package one.mixin.android.ui.conversation.link

import android.annotation.SuppressLint
import android.app.Dialog
import android.net.Uri
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
import one.mixin.android.Constants
import one.mixin.android.Constants.Scheme
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.TransferRequest
import one.mixin.android.api.response.AuthorizationResponse
import one.mixin.android.api.response.ConversationResponse
import one.mixin.android.api.response.MultisigsResponse
import one.mixin.android.api.response.NonFungibleOutputResponse
import one.mixin.android.api.response.PaymentCodeResponse
import one.mixin.android.api.response.PaymentStatus
import one.mixin.android.api.response.getScopes
import one.mixin.android.api.response.signature.SignatureState
import one.mixin.android.databinding.FragmentBottomSheetBinding
import one.mixin.android.extension.appendQueryParamsFromOtherUri
import one.mixin.android.extension.base64RawURLDecode
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.getGroupAvatarPath
import one.mixin.android.extension.handleSchemeSend
import one.mixin.android.extension.isExternalScheme
import one.mixin.android.extension.isExternalTransferUrl
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.isUUID
import one.mixin.android.extension.stripAmountZero
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.SyncOutputJob
import one.mixin.android.job.getIconUrlName
import one.mixin.android.repository.QrCodeType
import one.mixin.android.session.Session
import one.mixin.android.tip.TAG_TIP_SIGN
import one.mixin.android.tip.Tip
import one.mixin.android.tip.TipSignAction
import one.mixin.android.tip.matchTipSignAction
import one.mixin.android.ui.auth.AuthBottomSheetDialogFragment
import one.mixin.android.ui.common.JoinGroupBottomSheetDialogFragment
import one.mixin.android.ui.common.JoinGroupConversation
import one.mixin.android.ui.common.PinInputBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.AddressManageBiometricItem
import one.mixin.android.ui.common.biometric.SafeMultisigsBiometricItem
import one.mixin.android.ui.common.showUserBottom
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.device.ConfirmBottomFragment
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.ui.oldwallet.BottomSheetViewModel
import one.mixin.android.ui.oldwallet.MultisigsBottomSheetDialogFragment
import one.mixin.android.ui.oldwallet.NftBottomSheetDialogFragment
import one.mixin.android.ui.oldwallet.OldTransferFragment
import one.mixin.android.ui.oldwallet.OutputBottomSheetDialogFragment
import one.mixin.android.ui.oldwallet.PreconditionBottomSheetDialogFragment
import one.mixin.android.ui.oldwallet.PreconditionBottomSheetDialogFragment.Companion.FROM_LINK
import one.mixin.android.ui.oldwallet.TransactionBottomSheetDialogFragment
import one.mixin.android.ui.oldwallet.biometric.AssetBiometricItem
import one.mixin.android.ui.oldwallet.biometric.Multi2MultiBiometricItem
import one.mixin.android.ui.oldwallet.biometric.NftBiometricItem
import one.mixin.android.ui.oldwallet.biometric.One2MultiBiometricItem
import one.mixin.android.ui.oldwallet.biometric.TransferBiometricItem
import one.mixin.android.ui.url.UrlInterpreterActivity
import one.mixin.android.ui.wallet.transfer.TransferBottomSheetDialogFragment
import one.mixin.android.ui.web.WebActivity
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.SystemUIManager
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.User
import one.mixin.android.vo.generateConversationId
import one.mixin.android.vo.safe.TokenItem
import timber.log.Timber
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.SocketTimeoutException
import java.net.URLDecoder
import java.net.UnknownHostException
import java.nio.charset.StandardCharsets.UTF_8
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class LinkBottomSheetDialogFragment : BottomSheetDialogFragment() {
    companion object {
        const val TAG = "LinkBottomSheetDialogFragment"
        const val CODE = "code"
        const val FROM = "from"

        const val FROM_EXTERNAL = 0
        const val FROM_INTERNAL = 1
        const val FROM_SCAN = 2

        fun newInstance(
            code: String,
            from: Int = FROM_INTERNAL,
        ) =
            LinkBottomSheetDialogFragment().withArgs {
                putString(CODE, code)
                putInt(FROM, from)
            }
    }

    @Inject
    lateinit var tip: Tip

    @Inject
    lateinit var jobManager: MixinJobManager

    private var authOrPay = false

    override fun getTheme() = R.style.AppTheme_Dialog

    private val oldLinkViewModel by viewModels<BottomSheetViewModel>()

    val linkViewModel by viewModels<one.mixin.android.ui.common.BottomSheetViewModel>()

    private val binding by viewBinding(FragmentBottomSheetBinding::inflate)

    private lateinit var code: String
    private lateinit var contentView: View

    private lateinit var url: String
    private val from: Int by lazy { requireArguments().getInt(FROM, FROM_EXTERNAL) }

    private val newSchemeParser: NewSchemeParser by lazy { NewSchemeParser(this) }

    override fun onStart() {
        try {
            super.onStart()
        } catch (ignored: WindowManager.BadTokenException) {
        }
        dialog?.window?.let { window ->
            SystemUIManager.lightUI(
                window,
                !requireContext().booleanFromAttribute(R.attr.flag_night),
            )
        }
    }

    private fun getUserOrAppNotFoundTip(isApp: Boolean) = if (isApp) R.string.Bot_not_found else R.string.User_not_found

    @SuppressLint("RestrictedApi")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        dialog.window?.let { window ->
            SystemUIManager.lightUI(window, requireContext().isNightMode())
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

        url = requireNotNull(requireArguments().getString(CODE)) { "required url can not be null" }
        parseUrl(url)
    }

    private fun parseUrl(url: String) {
        val isUserScheme = url.startsWith(Scheme.USERS, true) || url.startsWith(Scheme.HTTPS_USERS, true)
        val isAppScheme = url.startsWith(Scheme.APPS, true) || url.startsWith(Scheme.HTTPS_APPS, true)
        if (isUserScheme || isAppScheme) {
            val uri = url.toUri()
            val segments = uri.pathSegments
            if (segments.isEmpty()) return

            val userId =
                if (segments.size >= 2) {
                    segments[1]
                } else {
                    segments[0]
                }
            if (!userId.isUUID()) {
                toast(getUserOrAppNotFoundTip(isAppScheme))
                dismiss()
            } else {
                lifecycleScope.launch(errorHandler) {
                    val user = oldLinkViewModel.refreshUser(userId)
                    if (user == null) {
                        toast(getUserOrAppNotFoundTip(isAppScheme))
                        dismiss()
                        return@launch
                    }
                    val isOpenApp = isAppScheme && uri.getQueryParameter("action") == "open"
                    if (isOpenApp && user.appId != null) {
                        lifecycleScope.launch(errorHandler) {
                            val app = oldLinkViewModel.findAppById(user.appId!!)
                            if (app != null) {
                                val url =
                                    try {
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

            val userId =
                if (segments.size >= 2) {
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
                    val user = oldLinkViewModel.refreshUser(userId)
                    if (user == null) {
                        toast(R.string.User_not_found)
                        dismiss()
                        return@launch
                    }
                    OldTransferFragment.newInstance(userId, supportSwitchAsset = true)
                        .showNow(parentFragmentManager, OldTransferFragment.TAG)
                    dismiss()
                }
            }
        } else if (url.startsWith(Scheme.HTTPS_MULTISIGS, true) || url.startsWith(Scheme.MIXIN_MULTISIGS, true)) {
            if (checkHasPin()) return
            lifecycleScope.launch(errorHandler) {
                val uri = Uri.parse(url)
                val segments = Uri.parse(url).pathSegments
                if (segments.isEmpty()) return@launch
                val requestId = segments[1]
                if (!requestId.isUUID()) {
                    showError(R.string.Invalid_payment_link)
                }
                val action = uri.getQueryParameter("action") ?: "sign"
                if (!action.equals("sign", true) && !action.equals("unlock", true)) {
                    showError()
                    return@launch
                }
                val transactionResponse = linkViewModel.getMultisigs(requestId)
                if (!transactionResponse.isSuccess) {
                    showError()
                    return@launch
                }
                val multisigs = transactionResponse.data!!
                val asset = checkToken(multisigs.assetId!!)
                if (asset == null) {
                    showError()
                    return@launch
                }
                var state: String = SignatureState.initial.name
                multisigs.signers?.let { signers ->
                    when {
                        signers.size >= multisigs.sendersThreshold -> state = PaymentStatus.paid.name
                        action == "sign" && Session.getAccountId() in signers -> state = SignatureState.signed.name
                        action == "unlock" && signers.isEmpty() -> state = SignatureState.unlocked.name
                    }
                }
                val sendersHash = multisigs.sendersHash
                val receiver =
                    multisigs.receivers?.firstOrNull {
                        it.membersHash != sendersHash
                    } ?: multisigs.receivers?.firstOrNull {
                        it.membersHash == sendersHash
                    }
                if (receiver == null || receiver.members.isEmpty()) {
                    showError()
                    return@launch
                }
                val multisigsBiometricItem =
                    SafeMultisigsBiometricItem(
                        action = action,
                        traceId = multisigs.requestId,
                        senders = multisigs.senders.toTypedArray(),
                        receivers = receiver.members.toTypedArray(),
                        sendersThreshold = multisigs.sendersThreshold,
                        receiverThreshold = receiver.threshold,
                        asset = asset,
                        amount = multisigs.amount,
                        memo = null,
                        raw = multisigs.rawTransaction,
                        index = multisigs.senders.indexOf(Session.getAccountId()),
                        views = if (multisigs.views.isNullOrEmpty()) null else multisigs.views.joinToString(","),
                        state = state,
                    )
                TransferBottomSheetDialogFragment.newInstance(multisigsBiometricItem).showNow(
                    parentFragmentManager,
                    TransferBottomSheetDialogFragment.TAG,
                )
                dismiss()
            }
        } else if (url.startsWith(Scheme.MIXIN_PAY)) {
            if (checkHasPin()) return
            lifecycleScope.launch(errorHandler) {
                val r = newSchemeParser.parse(url, from)
                if (r.isSuccess) {
                    dismiss()
                } else {
                    val e = r.exceptionOrNull()
                    if (e is ParserError && e.symbol != null) {
                        showError("${e.symbol} ${getString(R.string.insufficient_balance)}")
                    } else {
                        showError(getString(R.string.Invalid_payment_link))
                    }
                }
            }
        } else if (url.startsWith(Scheme.HTTPS_PAY, true) || url.startsWith(Scheme.PAY, true)) {
            if (checkHasPin()) return

            lifecycleScope.launch(errorHandler) {
                val uri = url.toUri()
                val segments = uri.pathSegments
                if (segments.size == (if (url.startsWith(Scheme.HTTPS_PAY, true)) 1 else 0)) {
                    if (!showOldTransfer(url)) {
                        showError(R.string.Invalid_payment_link)
                    }
                } else if (segments.size == (if (url.startsWith(Scheme.HTTPS_PAY, true)) 2 else 1)) {
                    val r = newSchemeParser.parse(url, from)
                    if (r.isSuccess) {
                        dismiss()
                    } else {
                        val e = r.exceptionOrNull()
                        if (e is ParserError && e.symbol != null) {
                            showError("${e.symbol} ${getString(R.string.insufficient_balance)}")
                        } else {
                            showError(getString(R.string.Invalid_payment_link))
                        }
                    }
                } else {
                    dismiss()
                }
            }
        } else if (url.startsWith(Scheme.HTTPS_SCHEME, true) || url.startsWith(Scheme.MIXIN_SCHEME, true)) {
            val segments = Uri.parse(url).pathSegments
            if (segments.isEmpty()) return

            if (segments.size < 2) {
                showError()
                return
            }
            val uuid = segments[1]
            if (!uuid.isUUID()) {
                showError()
                return
            }
            lifecycleScope.launch(errorHandler) {
                val scheme = linkViewModel.getScheme(uuid).data
                if (scheme == null) {
                    showError()
                    return@launch
                }
                parseUrl(scheme.target)
            }
        } else if (url.startsWith(Scheme.HTTPS_CODES, true) || url.startsWith(Scheme.CODES, true)) {
            val segments = Uri.parse(url).pathSegments
            if (segments.isEmpty()) return

            code =
                if (segments.size >= 2) {
                    segments[1]
                } else {
                    segments[0]
                }
            lifecycleScope.launch(errorHandler) {
                val result = oldLinkViewModel.searchCode(code)
                when (result.first) {
                    QrCodeType.conversation.name -> {
                        val response = result.second as ConversationResponse
                        val found =
                            response.participants.find { it.userId == Session.getAccountId() }
                        if (found != null) {
                            oldLinkViewModel.refreshConversation(response.conversationId)
                            toast(R.string.group_already_in)
                            context?.let { ConversationActivity.show(it, response.conversationId) }
                            dismiss()
                        } else {
                            val avatarUserIds = mutableListOf<String>()
                            val notExistsUserIdList = mutableListOf<String>()
                            for (p in response.participants) {
                                val u = oldLinkViewModel.suspendFindUserById(p.userId)
                                if (u == null) {
                                    notExistsUserIdList.add(p.userId)
                                }
                                if (avatarUserIds.size < 4) {
                                    avatarUserIds.add(p.userId)
                                }
                            }
                            val avatar4List = avatarUserIds.take(4)
                            val iconUrl =
                                if (notExistsUserIdList.isNotEmpty()) {
                                    oldLinkViewModel.refreshUsers(
                                        notExistsUserIdList,
                                        response.conversationId,
                                    )
                                    null
                                } else {
                                    val avatarUsers =
                                        oldLinkViewModel.findMultiUsersByIds(avatar4List.toSet())
                                    oldLinkViewModel.startGenerateAvatar(
                                        response.conversationId,
                                        avatar4List,
                                    )

                                    val name = getIconUrlName(response.conversationId, avatarUsers)
                                    val f = requireContext().getGroupAvatarPath(name, false)
                                    f.absolutePath
                                }
                            val joinGroupConversation =
                                JoinGroupConversation(
                                    response.conversationId,
                                    response.name,
                                    response.announcement,
                                    response.participants.size,
                                    iconUrl,
                                )
                            JoinGroupBottomSheetDialogFragment.newInstance(
                                joinGroupConversation,
                                code,
                            ).showNow(
                                parentFragmentManager,
                                JoinGroupBottomSheetDialogFragment.TAG,
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

                        activity?.let {
                            if (it.isFinishing) return@let
                            it.supportFragmentManager.findFragmentByTag(AuthBottomSheetDialogFragment.TAG)?.let { f ->
                                (f as? AuthBottomSheetDialogFragment)?.dismiss()
                            }
                            val scopes = authorization.getScopes(it)
                            AuthBottomSheetDialogFragment.newInstance(
                                scopes,
                                authorization.app.name,
                                authorization.app.appNumber,
                                authorization.app.iconUrl,
                                authorization.authorizationId,
                            )
                                .showNow(
                                    parentFragmentManager,
                                    AuthBottomSheetDialogFragment.TAG,
                                )
                            authOrPay = true
                            dismiss()
                        }
                    }
                    QrCodeType.multisig_request.name -> {
                        if (checkHasPin()) return@launch

                        val multisigs = result.second as MultisigsResponse
                        val asset = checkAsset(multisigs.assetId)
                        if (asset != null) {
                            val multisigsBiometricItem =
                                Multi2MultiBiometricItem(
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
                                    state = multisigs.state,
                                )
                            MultisigsBottomSheetDialogFragment.newInstance(
                                multisigsBiometricItem,
                            ).showNow(
                                parentFragmentManager,
                                MultisigsBottomSheetDialogFragment.TAG,
                            )
                            dismiss()
                        } else {
                            showError()
                        }
                    }
                    QrCodeType.non_fungible_request.name -> {
                        if (checkHasPin()) return@launch
                        val nfoResponse = result.second as NonFungibleOutputResponse
                        val nftBiometricItem =
                            NftBiometricItem(
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
                                state = nfoResponse.state,
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
                            if (paymentCodeResponse.receivers.isEmpty()) {
                                showError()
                            } else if (paymentCodeResponse.receivers.size == 1 && paymentCodeResponse.threshold == 1) { // Transfer when there is only one recipient
                                val user = oldLinkViewModel.refreshUser(paymentCodeResponse.receivers.first())
                                if (user == null) {
                                    showError(R.string.User_not_found)
                                    return@launch
                                }
                                showOldTransferBottom(user, paymentCodeResponse.amount, asset, paymentCodeResponse.traceId, paymentCodeResponse.status, paymentCodeResponse.memo, null)
                            } else if (paymentCodeResponse.receivers.size > 1) {
                                val multisigsBiometricItem =
                                    One2MultiBiometricItem(
                                        threshold = paymentCodeResponse.threshold,
                                        senders = arrayOf(Session.getAccountId()!!),
                                        receivers = paymentCodeResponse.receivers,
                                        asset = asset,
                                        amount = paymentCodeResponse.amount,
                                        pin = null,
                                        traceId = paymentCodeResponse.traceId,
                                        memo = paymentCodeResponse.memo,
                                        state = paymentCodeResponse.status,
                                    )
                                MultisigsBottomSheetDialogFragment.newInstance(
                                    multisigsBiometricItem,
                                ).showNow(
                                    parentFragmentManager,
                                    MultisigsBottomSheetDialogFragment.TAG,
                                )
                            }
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
                        val pair = oldLinkViewModel.findAddressById(addressId, assetId)
                        val address = pair.first
                        if (pair.second) {
                            showError(R.string.error_address_exists)
                        } else if (address == null) {
                            showError(R.string.error_address_not_sync)
                        } else {
                            val asset = checkToken(assetId)
                            if (asset != null) {
                                TransferBottomSheetDialogFragment.newInstance(
                                    AddressManageBiometricItem(
                                        asset = asset,
                                        addressId = addressId,
                                        label = address.label,
                                        destination = address.destination,
                                        tag = address.tag,
                                        type = TransferBottomSheetDialogFragment.DELETE,
                                    ),
                                ).showNow(this@LinkBottomSheetDialogFragment.parentFragmentManager, TransferBottomSheetDialogFragment.TAG)
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
                val label =
                    uri.getQueryParameter("label").run {
                        Uri.decode(this)
                    }
                val tag =
                    uri.getQueryParameter("tag").run {
                        Uri.decode(this)
                    }
                if (assetId != null && assetId.isUUID() && !destination.isNullOrEmpty() && !label.isNullOrEmpty()) {
                    lifecycleScope.launch(errorHandler) {
                        val asset = checkToken(assetId)
                        if (asset != null) {
                            TransferBottomSheetDialogFragment.newInstance(
                                AddressManageBiometricItem(
                                    asset = asset,
                                    label = label,
                                    destination = destination,
                                    addressId = null,
                                    tag = tag,
                                    type = TransferBottomSheetDialogFragment.ADD,
                                ),
                            ).showNow(this@LinkBottomSheetDialogFragment.parentFragmentManager, TransferBottomSheetDialogFragment.TAG)
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
                    val result = oldLinkViewModel.getSnapshotByTraceId(traceId)
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
                    val result = oldLinkViewModel.getSnapshotAndAsset(snapshotId)
                    if (result != null) {
                        dismiss()
                        TransactionBottomSheetDialogFragment.newInstance(result.first, result.second)
                            .show(parentFragmentManager, TransactionBottomSheetDialogFragment.TAG)
                    } else {
                        showError()
                    }
                }
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
                    val user = oldLinkViewModel.refreshUser(userId)
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
                    val conversation = oldLinkViewModel.getAndSyncConversation(conversationId)
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
                },
            )
        } else if (url.startsWith(Scheme.DEVICE, true)) {
            contentView.post {
                ConfirmBottomFragment.show(requireContext(), parentFragmentManager, url)
                dismiss()
            }
        } else if (url.startsWith(Scheme.BUY, true)) {
            MainActivity.showWallet(requireContext(), buy = true)
            dismiss()
        } else if (url.startsWith(Scheme.TIP, true)) {
            val uri = Uri.parse(url)
            handleTipScheme(uri)
        } else {
            val isExternalTransferUrl = url.isExternalTransferUrl()
            if (isExternalTransferUrl) {
                if (checkHasPin()) return

                lifecycleScope.launch(errorHandler) {
                    newSchemeParser.parseExternalTransferUrl(url)
                }
            } else if (url.isExternalScheme(requireContext())) {
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

    override fun showNow(
        manager: FragmentManager,
        tag: String?,
    ) {
        try {
            super.showNow(manager, tag)
        } catch (e: IllegalStateException) {
            Timber.w(e)
        }
    }

    override fun onDetach() {
        super.onDetach()
        // UrlInterpreterActivity doesn't have a UI and needs it's son fragment to handle it's finish.
        if (activity is UrlInterpreterActivity) {
            var realFragmentCount = 0
            parentFragmentManager.fragments.forEach { f ->
                if (f !is SupportRequestManagerFragment) {
                    realFragmentCount++
                }
            }
            if (realFragmentCount <= 0) {
                if (isAdded) {
                    activity?.finish()
                }
            }
        }
    }

    private fun handleTipScheme(uri: Uri) {
        val segments = uri.pathSegments
        if (segments.isEmpty()) return

        val action = segments[0]
        if (action.isBlank()) {
            showError()
            return
        }
        val id = uri.getQueryParameter("id")
        if (id.isNullOrBlank() || !id.isUUID()) {
            showError()
            return
        }
        val alg = uri.getQueryParameter("alg")
        val crv = uri.getQueryParameter("crv")
        if (alg.isNullOrBlank() || crv.isNullOrBlank()) {
            showError()
            return
        }
        val signAction = matchTipSignAction(action, alg, crv)
        if (signAction == null) {
            showError()
            return
        }
        val data: ByteArray? =
            if (signAction is TipSignAction.Signature) {
                val d = uri.getQueryParameter("data")
                if (d.isNullOrBlank()) {
                    showError()
                    return
                } else {
                    d.base64RawURLDecode()
                }
            } else {
                null
            }

        contentView.post {
            PinInputBottomSheetDialogFragment.newInstance().setOnPinComplete { pin ->
                lifecycleScope.launch(errorHandler) {
                    tip.getOrRecoverTipPriv(requireContext(), pin)
                        .onSuccess { priv ->
                            val res: String =
                                when (signAction) {
                                    is TipSignAction.Public -> {
                                        val pub = signAction(priv)
                                        Timber.d("$TAG_TIP_SIGN pub: $pub")
                                        pub
                                    }
                                    is TipSignAction.Signature -> {
                                        val sig = signAction(priv, requireNotNull(data) { "Signature action data can not be null" })
                                        Timber.d("$TAG_TIP_SIGN sig: $sig")
                                        sig
                                    }
                                }
                            val endpoint = uri.getQueryParameter("notify") ?: Constants.API.DEFAULT_TIP_SIGN_ENDPOINT
                            val notifyUrl = "$endpoint?id=$id&res=$res"
                            Timber.d("$TAG_TIP_SIGN notify url: $notifyUrl")
                        }.onFailure {
                            Timber.d("$TAG_TIP_SIGN ${it.stackTraceToString()}")
                        }
                }
            }.showNow(parentFragmentManager, PinInputBottomSheetDialogFragment.TAG)
            dismiss()
        }
    }

    private suspend fun showOldTransfer(text: String): Boolean {
        val uri = text.toUri()
        val amount = uri.getQueryParameter("amount")?.stripAmountZero() ?: return false
        if (amount.toBigDecimalOrNull() == null) return false
        val userId = uri.getQueryParameter("recipient")
        if (userId == null || !userId.isUUID()) {
            return false
        }
        val assetId = uri.getQueryParameter("asset")
        if (assetId == null || !assetId.isUUID()) {
            return false
        }
        val trace = uri.getQueryParameter("trace") ?: UUID.randomUUID().toString()
        val memo =
            uri.getQueryParameter("memo")?.run {
                Uri.decode(this)
            }
        val returnTo =
            uri.getQueryParameter("return_to")?.run {
                if (from == FROM_EXTERNAL) {
                    try {
                        URLDecoder.decode(this, UTF_8.name())
                    } catch (e: UnsupportedEncodingException) {
                        this
                    }
                } else {
                    null
                }
            }

        val asset: AssetItem = checkAsset(assetId) ?: return false

        val user = oldLinkViewModel.refreshUser(userId) ?: return false

        val transferRequest = TransferRequest(assetId, userId, amount, null, trace, memo)
        return handleMixinResponse(
            invokeNetwork = {
                oldLinkViewModel.paySuspend(transferRequest)
            },
            successBlock = { r ->
                val response = r.data ?: return@handleMixinResponse false
                showOldTransferBottom(user, amount, asset, trace, response.status, memo, returnTo)
                return@handleMixinResponse true
            },
        ) ?: false
    }

    private suspend fun showOldTransferBottom(
        user: User,
        amount: String,
        asset: AssetItem,
        traceId: String,
        status: String,
        memo: String?,
        returnTo: String?,
    ) {
        val pair = oldLinkViewModel.findLatestTrace(user.userId, null, null, amount, asset.assetId)
        if (pair.second) {
            showError(getString(R.string.check_trace_failed))
            return
        }
        val biometricItem = TransferBiometricItem(user, asset, amount, null, traceId, memo, status, pair.first, returnTo)
        showOldPreconditionBottom(biometricItem)
    }

    private fun showOldPreconditionBottom(biometricItem: AssetBiometricItem) {
        val preconditionBottom = PreconditionBottomSheetDialogFragment.newInstance(biometricItem, FROM_LINK)
        preconditionBottom.callback =
            object : PreconditionBottomSheetDialogFragment.Callback {
                override fun onSuccess() {
                    val bottom = OutputBottomSheetDialogFragment.newInstance(biometricItem)
                    bottom.show(preconditionBottom.parentFragmentManager, OutputBottomSheetDialogFragment.TAG)
                    dismiss()
                }

                override fun onCancel() {
                    dismiss()
                }
            }
        preconditionBottom.showNow(parentFragmentManager, PreconditionBottomSheetDialogFragment.TAG)
    }

    private suspend fun checkAsset(assetId: String): AssetItem? {
        var asset = oldLinkViewModel.findAssetItemById(assetId)
        if (asset == null) {
            asset = oldLinkViewModel.refreshAsset(assetId)
        }
        if (asset != null && asset.assetId != asset.chainId && oldLinkViewModel.findAssetItemById(asset.chainId) == null) {
            oldLinkViewModel.refreshAsset(asset.chainId)
        }
        return oldLinkViewModel.findAssetItemById(assetId)
    }

    private suspend fun checkToken(assetId: String): TokenItem? {
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
    fun showError(
        @StringRes errorRes: Int = R.string.Invalid_Link,
    ) {
        if (!isAdded) return

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

    fun showError(error: String) {
        if (!isAdded) return

        binding.apply {
            linkErrorInfo.text = error
            linkLoading.visibility = GONE
            linkErrorInfo.visibility = VISIBLE
        }
    }

    private val mBottomSheetBehaviorCallback =
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(
                bottomSheet: View,
                newState: Int,
            ) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    try {
                        dismissAllowingStateLoss()
                    } catch (e: IllegalStateException) {
                        Timber.w(e)
                    }
                }
            }

            override fun onSlide(
                bottomSheet: View,
                slideOffset: Float,
            ) {}
        }

    fun syncUtxo() {
        jobManager.addJobInBackground(SyncOutputJob())
    }

    private val errorHandler =
        CoroutineExceptionHandler { _, error ->
            when (error) {
                is SocketTimeoutException -> showError(R.string.error_connection_timeout)
                is UnknownHostException -> showError(R.string.No_network_connection)
                is IOException -> showError(R.string.No_network_connection)
                else -> {
                    ErrorHandler.handleError(error)
                    Timber.e(error)
                    showError(R.string.Network_error)
                }
            }
        }
}
