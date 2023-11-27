package one.mixin.android.ui.common.biometric

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import one.mixin.android.api.response.PaymentStatus
import one.mixin.android.ui.wallet.NetworkFee
import one.mixin.android.vo.Address
import one.mixin.android.vo.Trace
import one.mixin.android.vo.User
import one.mixin.android.vo.safe.TokenItem

@Parcelize
open class BiometricItem(
    open val amount: String,
    open var pin: String?,
    open val memo: String?,
    open val state: String,
) : Parcelable

@Parcelize
open class AssetBiometricItem(
    open var asset: TokenItem?,
    open var traceId: String?,
    override var amount: String,
    override var pin: String?,
    override var memo: String?,
    override var state: String,
) : BiometricItem(amount, pin, memo, state)

@Parcelize
class TransferBiometricItem(
    var users: List<User>,
    val threshold: Byte,
    override var asset: TokenItem?,
    override var amount: String,
    override var pin: String?,
    override var traceId: String?,
    override var memo: String?,
    override var state: String,
    var trace: Trace?,
    val returnTo: String?,
) : AssetBiometricItem(asset, traceId, amount, pin, memo, state)

fun buildEmptyTransferBiometricItem(user: User) =
    TransferBiometricItem(listOf(user), 1.toByte(), null, "", null, null, null, PaymentStatus.pending.name, null, null)

fun buildTransferBiometricItem(
    user: User,
    token: TokenItem?,
    amount: String,
    traceId: String?,
    memo: String?,
    returnTo: String?,
) =
    TransferBiometricItem(listOf(user), 1.toByte(), token, amount, null, traceId, memo, PaymentStatus.pending.name, null, returnTo)

@Parcelize
class AddressTransferBiometricItem(
    val address: String,
    override var asset: TokenItem?,
    override var amount: String,
    override var pin: String?,
    override var traceId: String?,
    override var memo: String?,
    override var state: String,
    val returnTo: String?,
) : AssetBiometricItem(asset, traceId, amount, pin, memo, state)

fun buildAddressBiometricItem(
    mainnetAddress: String,
    token: TokenItem?,
    amount: String,
    traceId: String?,
    memo: String?,
    returnTo: String?,
) =
    AddressTransferBiometricItem(mainnetAddress, token, amount, null, traceId, memo, PaymentStatus.pending.name, returnTo)

@Parcelize
class WithdrawBiometricItem(
    var address: Address,
    var fee: NetworkFee?,
    override var asset: TokenItem?,
    override var amount: String,
    override var pin: String?,
    override var traceId: String?,
    override var memo: String?,
    override var state: String,
    var trace: Trace?,
) : AssetBiometricItem(asset, traceId, amount, pin, memo, state)

fun WithdrawBiometricItem.displayAddress(): String {
    return if (!address.tag.isNullOrEmpty()) {
        "${address.destination}:${address.tag}"
    } else {
        address.destination
    }
}

fun buildWithdrawalBiometricItem(
    address: Address,
    asset: TokenItem,
) =
    WithdrawBiometricItem(address, null, asset, "", null, null, null, PaymentStatus.pending.name, null)

fun WithdrawBiometricItem.hasAddress() = address.addressId.isNotBlank()

@Parcelize
open class MultisigsBiometricItem(
    open val senders: Array<String>,
    open val receivers: Array<String>,
    open val threshold: Int,
    override var asset: TokenItem?,
    override var amount: String,
    override var pin: String?,
    override var traceId: String?,
    override var memo: String?,
    override var state: String,
) : AssetBiometricItem(asset, traceId, amount, pin, memo, state)

@Parcelize
class Multi2MultiBiometricItem(
    val requestId: String,
    val action: String,
    override val senders: Array<String>,
    override val receivers: Array<String>,
    override val threshold: Int,
    override var asset: TokenItem?,
    override var amount: String,
    override var pin: String?,
    override var traceId: String?,
    override var memo: String?,
    override var state: String,
) : MultisigsBiometricItem(senders, receivers, threshold, asset, amount, pin, traceId, memo, state)

@Parcelize
class One2MultiBiometricItem(
    override val senders: Array<String>,
    override val receivers: Array<String>,
    override val threshold: Int,
    override var asset: TokenItem?,
    override var amount: String,
    override var pin: String?,
    override var traceId: String?,
    override var memo: String?,
    override var state: String,
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
