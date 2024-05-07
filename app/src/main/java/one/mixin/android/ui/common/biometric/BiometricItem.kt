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
    open val reference: String?
) : Parcelable

@Parcelize
open class AssetBiometricItem(
    open var asset: TokenItem?,
    open val traceId: String,
    override var amount: String,
    override var memo: String?,
    override var state: String,
    override var reference: String?
) : BiometricItem(amount, memo, state, reference)

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
    override var reference: String?
) : AssetBiometricItem(asset, traceId, amount, memo, state, reference)

fun buildEmptyTransferBiometricItem(user: User) =
    TransferBiometricItem(listOf(user), 1.toByte(), UUID.randomUUID().toString(), null, "", null, PaymentStatus.pending.name, null, null, null)

fun buildTransferBiometricItem(
    user: User,
    token: TokenItem?,
    amount: String,
    traceId: String?,
    memo: String?,
    returnTo: String?,
    reference: String? = null,
) =
    TransferBiometricItem(listOf(user), 1.toByte(), traceId ?: UUID.randomUUID().toString(), token, amount, memo, PaymentStatus.pending.name, null, returnTo, reference)

@Parcelize
class AddressTransferBiometricItem(
    val address: String,
    override val traceId: String,
    override var asset: TokenItem?,
    override var amount: String,
    override var memo: String?,
    override var state: String,
    val returnTo: String?,
    override var reference: String?,
) : AssetBiometricItem(asset, traceId, amount, memo, state, reference)

class AddressManageBiometricItem(
    override var asset: TokenItem?,
    val destination: String?,
    val tag: String?,
    val addressId: String?,
    val label: String?,
    val type: Int,
) : AssetBiometricItem(asset, "", "0", null, "", null)

fun buildAddressBiometricItem(
    mainnetAddress: String,
    traceId: String?,
    token: TokenItem?,
    amount: String,
    memo: String?,
    returnTo: String?,
    from: Int,
    reference: String?
) =
    AddressTransferBiometricItem(mainnetAddress, traceId ?: UUID.randomUUID().toString(), token, amount, memo, PaymentStatus.pending.name, returnTo, reference)

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
) : AssetBiometricItem(asset, traceId, amount, memo, state, null)

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
    override var reference:String?
) : AssetBiometricItem(asset, traceId, amount, memo, state, reference)

@Parcelize
class NftBiometricItem(
    override var asset: TokenItem?,
    override val traceId: String,
    override var amount: String,
    override var memo: String?,
    override var state: String,
    override var reference: String?
) : AssetBiometricItem(asset, traceId, amount, memo, state, reference)
