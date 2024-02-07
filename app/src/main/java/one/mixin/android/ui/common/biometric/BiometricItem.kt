package one.mixin.android.ui.common.biometric

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import one.mixin.android.api.response.PaymentStatus
import one.mixin.android.ui.wallet.NetworkFee
import one.mixin.android.vo.Address
import one.mixin.android.vo.Trace
import one.mixin.android.vo.User
import one.mixin.android.vo.safe.TokenItem
import java.util.UUID

@Parcelize
open class BiometricItem(
    open val amount: String,
    open val memo: String?,
    open val state: String,
) : Parcelable

@Parcelize
open class AssetBiometricItem(
    open var asset: TokenItem?,
    open val traceId: String,
    override var amount: String,
    override var memo: String?,
    override var state: String,
) : BiometricItem(amount, memo, state)

@Parcelize
class TransferBiometricItem(
    var users: List<User>,
    val threshold: Byte,
    override val traceId: String,
    override var asset: TokenItem?,
    override var amount: String,
    override var memo: String?,
    override var state: String,
    var trace: Trace?,
    val returnTo: String?,
) : AssetBiometricItem(asset, traceId, amount, memo, state)

fun buildEmptyTransferBiometricItem(user: User) =
    TransferBiometricItem(listOf(user), 1.toByte(), UUID.randomUUID().toString(), null, "", null, PaymentStatus.pending.name, null, null)

fun buildTransferBiometricItem(
    user: User,
    token: TokenItem?,
    amount: String,
    traceId: String?,
    memo: String?,
    returnTo: String?,
) =
    TransferBiometricItem(listOf(user), 1.toByte(), traceId ?: UUID.randomUUID().toString(), token, amount, memo, PaymentStatus.pending.name, null, returnTo)

@Parcelize
class AddressTransferBiometricItem(
    val address: String,
    override val traceId: String,
    override var asset: TokenItem?,
    override var amount: String,
    override var memo: String?,
    override var state: String,
    val returnTo: String?,
) : AssetBiometricItem(asset, traceId, amount, memo, state)

fun buildAddressBiometricItem(
    mainnetAddress: String,
    traceId: String?,
    token: TokenItem?,
    amount: String,
    memo: String?,
    returnTo: String?,
    from: Int,
) =
    AddressTransferBiometricItem(mainnetAddress, traceId ?: UUID.randomUUID().toString(), token, amount, memo, PaymentStatus.pending.name, returnTo)

@Parcelize
class WithdrawBiometricItem(
    var address: Address,
    var fee: NetworkFee?,
    val label: String?,
    override val traceId: String,
    override var asset: TokenItem?,
    override var amount: String,
    override var memo: String?,
    override var state: String,
    var trace: Trace?,
) : AssetBiometricItem(asset, traceId, amount, memo, state)

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
    WithdrawBiometricItem(address, null, address.label, UUID.randomUUID().toString(), asset, "", null, PaymentStatus.pending.name, null)

fun WithdrawBiometricItem.hasAddress() = address.addressId.isNotBlank()

@Parcelize
open class SafeMultisigsBiometricItem(
    val action: String,
    val raw: String,
    val views: String?,
    val index: Int,
    open val senders: Array<String>,
    open val receivers: Array<String>,
    open val receiverThreshold: Int,
    open val sendersThreshold: Int,
    override val traceId: String,
    override var asset: TokenItem?,
    override var amount: String,
    override var memo: String?,
    override var state: String,
) : AssetBiometricItem(asset, traceId, amount, memo, state)

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
    override val memo: String?,
    override val state: String,
) : BiometricItem(amount, memo, state)
