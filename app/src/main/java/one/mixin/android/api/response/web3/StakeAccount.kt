package one.mixin.android.api.response.web3

import android.os.Parcelable
import java.math.BigInteger

import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class StakeAccount(
    val pubkey: String,
    val account: AccountInfo,
) : Parcelable

@Parcelize
data class AccountInfo(
    val data: Data,
    val executable: Boolean,
    val lamports: Long,
    val owner: String,
    val rentEpoch: BigInteger,
    val space: Int,
) : Parcelable

@Parcelize
data class Data(
    val parsed: Parsed,
    val program: String,
    val space: Int
) : Parcelable

@Parcelize
data class Parsed(
    val info: Info,
    val type: String
) : Parcelable

@Parcelize
data class Info(
    val meta: Meta,
    val stake: Stake
) : Parcelable

@Parcelize
data class Meta(
    val authorized: Authorized,
    val lockup: Lockup,
    @SerializedName("rentExemptReserve")
    val rentExemptReserve: String
) : Parcelable

@Parcelize
data class Authorized(
    val staker: String,
    val withdrawer: String
) : Parcelable

@Parcelize
data class Lockup(
    val custodian: String,
    val epoch: Int,
    @SerializedName("unixTimestamp")
    val unixTimestamp: Int
) : Parcelable

@Parcelize
data class Stake(
    @SerializedName("creditsObserved")
    val creditsObserved: Int,
    val delegation: Delegation
) : Parcelable

@Parcelize
data class Delegation(
    @SerializedName("activationEpoch")
    val activationEpoch: String,
    @SerializedName("deactivationEpoch")
    val deactivationEpoch: String,
    val stake: String,
    val voter: String,
    @SerializedName("warmupCooldownRate")
    val warmupCooldownRate: Double
) : Parcelable
