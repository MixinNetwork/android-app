package one.mixin.android.ui.common.biometric

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.User

@Parcelize
open class BiometricItem(
    val asset: AssetItem,
    val amount: String,
    var pin: String?,
    val trace: String?,
    val memo: String?
): Parcelable

class TransferBiometricItem(
    val user: User,
    asset: AssetItem,
    amount: String,
    pin: String?,
    trace: String?,
    memo: String?
) : BiometricItem(asset, amount, pin, trace, memo)

class WithdrawBiometricItem(
    val publicKey: String,
    val addressId: String,
    val label: String,
    asset: AssetItem,
    amount: String,
    pin: String?,
    trace: String?,
    memo: String?
) : BiometricItem(asset, amount, pin, trace, memo)