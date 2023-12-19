package one.mixin.android.ui.oldwallet.biometric

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import one.mixin.android.ui.conversation.link.LinkBottomSheetDialogFragment
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.Trace
import one.mixin.android.vo.User

@Parcelize
open class BiometricItem(
    open val amount: String,
    open var pin: String?,
    open val memo: String?,
    open val state: String,
) : Parcelable

@Parcelize
open class AssetBiometricItem(
    open val asset: AssetItem,
    open val traceId: String?,
    override val amount: String,
    override var pin: String?,
    override val memo: String?,
    override val state: String,
) : BiometricItem(amount, pin, memo, state)

@Parcelize
class TransferBiometricItem(
    val user: User,
    override val asset: AssetItem,
    override val amount: String,
    override var pin: String?,
    override val traceId: String?,
    override val memo: String?,
    override val state: String,
    val trace: Trace?,
    val returnTo: String?,
) : AssetBiometricItem(asset, traceId, amount, pin, memo, state)

@Parcelize
class WithdrawBiometricItem(
    val destination: String,
    val tag: String?,
    val addressId: String?,
    val label: String?,
    var fee: String,
    override val asset: AssetItem,
    override val amount: String,
    override var pin: String?,
    override val traceId: String?,
    override val memo: String?,
    override val state: String,
    val trace: Trace?,
) : AssetBiometricItem(asset, traceId, amount, pin, memo, state)

fun WithdrawBiometricItem.displayAddress(): String {
    return if (!tag.isNullOrEmpty()) {
        "$destination:$tag"
    } else {
        destination
    }
}

fun WithdrawBiometricItem.hasAddress() = addressId != null

@Parcelize
open class MultisigsBiometricItem(
    open val senders: Array<String>,
    open val receivers: Array<String>,
    open val threshold: Int,
    override val asset: AssetItem,
    override val amount: String,
    override var pin: String?,
    override val traceId: String?,
    override val memo: String?,
    override val state: String,
) : AssetBiometricItem(asset, traceId, amount, pin, memo, state)

@Parcelize
class Multi2MultiBiometricItem(
    val requestId: String,
    val action: String,
    override val senders: Array<String>,
    override val receivers: Array<String>,
    override val threshold: Int,
    override val asset: AssetItem,
    override val amount: String,
    override var pin: String?,
    override val traceId: String?,
    override val memo: String?,
    override val state: String,
) : MultisigsBiometricItem(senders, receivers, threshold, asset, amount, pin, traceId, memo, state)

@Parcelize
class One2MultiBiometricItem(
    override val senders: Array<String>,
    override val receivers: Array<String>,
    override val threshold: Int,
    override val asset: AssetItem,
    override val amount: String,
    override var pin: String?,
    override val traceId: String?,
    override val memo: String?,
    override val state: String,
) : MultisigsBiometricItem(senders, receivers, threshold, asset, amount, pin, traceId, memo, state)

@Parcelize
class NftBiometricItem(
    val requestId: String,
    val senders: Array<String>,
    val receivers: Array<String>,
    val sendersThreshold: Int,
    val receiversThreshold: Int,
    val tokenId: String,
    val action: String,
    val rawTransaction: String,
    override val amount: String,
    override var pin: String?,
    override val memo: String?,
    override val state: String,
) : BiometricItem(amount, pin, memo, state)
