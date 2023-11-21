package one.mixin.android.ui.conversation.link

import androidx.core.net.toUri
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.response.PaymentStatus
import one.mixin.android.extension.isUUID
import one.mixin.android.extension.stripAmountZero
import one.mixin.android.ui.common.OutputBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.AddressTransferBiometricItem
import one.mixin.android.ui.common.biometric.AssetBiometricItem
import one.mixin.android.ui.common.biometric.TransferBiometricItem
import one.mixin.android.ui.conversation.PreconditionBottomSheetDialogFragment
import one.mixin.android.ui.conversation.TransferFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.vo.MixAddressPrefix
import one.mixin.android.vo.safe.TokenItem
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.UUID

enum class PayType {
    Uuid,
    XinAddress,
    MixAddress,
}

class NewSchemaParser(
    private val bottomSheet: LinkBottomSheetDialogFragment,
) {
    private val linkViewModel = bottomSheet.linkViewModel

    suspend fun parse(text: String): Boolean {
        val uri = text.toUri()
        val lastPath = uri.lastPathSegment ?: return false

        val payType =
            if (lastPath.isUUID()) {
                PayType.Uuid
            } else if (lastPath.startsWith("XIN")) {
                PayType.XinAddress
            } else if (lastPath.startsWith(MixAddressPrefix)) {
                PayType.MixAddress
            } else {
                return false
            }
        val asset = uri.getQueryParameter("asset")
        if (asset != null && !asset.isUUID()) {
            return false
        }
        var amount = uri.getQueryParameter("amount")
        if (amount != null && amount.toBigDecimalOrNull() == null) {
            return false
        } else {
            amount = amount?.stripAmountZero()
        }
        val memo = uri.getQueryParameter("memo")
        val trace = uri.getQueryParameter("trace")
        if (trace != null && !trace.isUUID()) {
            return false
        }
        val returnTo =
            uri.getQueryParameter("return_to")?.run {
                try {
                    URLDecoder.decode(this, StandardCharsets.UTF_8.name())
                } catch (e: UnsupportedEncodingException) {
                    this
                }
            }

        if (payType == PayType.Uuid || payType == PayType.XinAddress) {
            if (asset != null && amount != null) {
                val traceId = trace ?: UUID.randomUUID().toString()
                var isTraceNotFound = false
                val tx =
                    handleMixinResponse(
                        invokeNetwork = { linkViewModel.getTransactionsById(traceId) },
                        successBlock = { r -> r.data },
                        failureBlock = {
                            isTraceNotFound = it.errorCode == ErrorHandler.NOT_FOUND
                            return@handleMixinResponse isTraceNotFound
                        },
                    )
                val status =
                    if (isTraceNotFound) {
                        PaymentStatus.pending.name
                    } else if (tx != null) {
                        PaymentStatus.paid.name
                    } else {
                        return false
                    }

                val token: TokenItem = checkToken(asset) ?: return false // TODO 404?
                if (payType == PayType.Uuid) {
                    val user = linkViewModel.refreshUser(lastPath) ?: return false

                    val biometricItem = TransferBiometricItem(user, token, amount, null, traceId, memo, status, null, returnTo)
                    showPreconditionBottom(biometricItem)
                } else {
                    // TODO verify address?
                    val addressTransferBiometricItem = AddressTransferBiometricItem(lastPath, token, amount, null, traceId, memo, status, returnTo)
                    showPreconditionBottom(addressTransferBiometricItem)
                }
            } else {
                if (payType == PayType.Uuid) {
                    TransferFragment.newInstance(
                        userId = lastPath,
                        amount = amount,
                        memo = memo,
                        trace = trace,
                        returnTo = returnTo,
                        supportSwitchAsset = true,
                    )
                } else {
                    TransferFragment.newInstance(
                        mainnetAddress = lastPath,
                        amount = amount,
                        memo = memo,
                        trace = trace,
                        returnTo = returnTo,
                        supportSwitchAsset = true,
                    )
                }.show(bottomSheet.parentFragmentManager, TransferFragment.TAG)
            }
        } else {
            // TODO
//            val mixAddress = lastPath.toMixAddress() ?: return false
            return false
        }
        return true
    }

    private suspend fun showPreconditionBottom(biometricItem: AssetBiometricItem) {
        if (biometricItem is TransferBiometricItem) {
            val pair = linkViewModel.findLatestTrace(biometricItem.user.userId, null, null, biometricItem.amount, biometricItem.asset.assetId)
            if (pair.second) {
                bottomSheet.showError(bottomSheet.getString(R.string.check_trace_failed))
                return
            }
            biometricItem.trace = pair.first
        }
        val preconditionBottom = PreconditionBottomSheetDialogFragment.newInstance(biometricItem, PreconditionBottomSheetDialogFragment.FROM_LINK)
        preconditionBottom.callback =
            object : PreconditionBottomSheetDialogFragment.Callback {
                override fun onSuccess() {
                    val bottom = OutputBottomSheetDialogFragment.newInstance(biometricItem)
                    bottom.show(preconditionBottom.parentFragmentManager, OutputBottomSheetDialogFragment.TAG)
                    bottomSheet.dismiss()
                }

                override fun onCancel() {
                    bottomSheet.dismiss()
                }
            }
        preconditionBottom.showNow(bottomSheet.parentFragmentManager, PreconditionBottomSheetDialogFragment.TAG)
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
}
