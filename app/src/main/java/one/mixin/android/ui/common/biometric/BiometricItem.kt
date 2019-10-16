package one.mixin.android.ui.common.biometric

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.User

@Parcelize
open class BiometricItem(
    open val asset: AssetItem,
    open val amount: String,
    open var pin: String?,
    open val trace: String?,
    open val tag: String?
) : Parcelable

@Parcelize
class TransferBiometricItem(
    val user: User,
    override val asset: AssetItem,
    override val amount: String,
    override var pin: String?,
    override val trace: String?,
    override val tag: String?
) : BiometricItem(asset, amount, pin, trace, tag)

@Parcelize
class WithdrawBiometricItem(
    val destination: String,
    val addressId: String,
    val label: String,
    override val asset: AssetItem,
    override val amount: String,
    override var pin: String?,
    override val trace: String?,
    override val tag: String?
) : BiometricItem(asset, amount, pin, trace, tag)
