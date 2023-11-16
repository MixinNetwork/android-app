package one.mixin.android.ui.conversation.link

import androidx.core.net.toUri
import one.mixin.android.api.response.PaymentStatus
import one.mixin.android.extension.isUUID
import one.mixin.android.extension.stripAmountZero
import one.mixin.android.ui.common.OutputBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.AddressTransferBiometricItem
import one.mixin.android.ui.common.biometric.AssetBiometricItem
import one.mixin.android.ui.common.biometric.TransferBiometricItem
import one.mixin.android.ui.conversation.PreconditionBottomSheetDialogFragment
import one.mixin.android.ui.conversation.TransferFragment
import one.mixin.android.util.decodeBase58
import one.mixin.android.vo.safe.TokenItem
import tip.Tip

enum class PayType {
    Uuid, XinAddress, MixAddress,
}

class NewSchemaParser(
    private val bottomSheet: LinkBottomSheetDialogFragment,
) {
    private val linkViewModel = bottomSheet.linkViewModel

    suspend fun parse(text: String): Boolean {
        val uri = text.toUri()
        val lastPath = uri.lastPathSegment ?: return false

        val payType = if (lastPath.isUUID()) {
            PayType.Uuid
        } else if (lastPath.startsWith("XIN")) {
            PayType.XinAddress
        } else if (lastPath.startsWith("MIX")) {
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

        if (payType == PayType.Uuid || payType == PayType.XinAddress) {
            if (asset != null && amount != null) {
                val token: TokenItem = checkToken(asset) ?: return false // TODO 404?
                if (payType == PayType.Uuid) {
                    val user = linkViewModel.refreshUser(lastPath) ?: return false
                    val biometricItem = TransferBiometricItem(user, token, amount, null, trace, memo, PaymentStatus.pending.name, null, null)
                    showPreconditionBottom(biometricItem)
                } else {
                    // TODO verify address?
                    val addressTransferBiometricItem = AddressTransferBiometricItem(lastPath, token, amount, null, trace, memo, PaymentStatus.pending.name)
                    showPreconditionBottom(addressTransferBiometricItem)
                }
            } else {
                if (payType == PayType.Uuid) {
                    TransferFragment.newInstance(
                        userId = lastPath,
                        amount = amount,
                        memo = memo,
                        trace = trace,
                        supportSwitchAsset = true,
                    )
                } else {
                    TransferFragment.newInstance(
                        mainnetAddress = lastPath,
                        amount = amount,
                        memo = memo,
                        trace = trace,
                        supportSwitchAsset = true,
                    )
                }.show(bottomSheet.parentFragmentManager, TransferFragment.TAG)
            }
        } else {
            val b = lastPath.removePrefix("MIX")
            val data = b.decodeBase58()
            // TODO
            return false
        }
        return true
    }

    private fun showPreconditionBottom(biometricItem: AssetBiometricItem) {
        val preconditionBottom = PreconditionBottomSheetDialogFragment.newInstance(biometricItem, PreconditionBottomSheetDialogFragment.FROM_LINK)
        preconditionBottom.callback = object : PreconditionBottomSheetDialogFragment.Callback {
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