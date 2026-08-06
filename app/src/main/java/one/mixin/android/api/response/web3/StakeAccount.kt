package one.mixin.android.api.response.web3

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.math.BigInteger

@Parcelize
data class StakeAccount(
    @SerializedName("pubkey")
    val pubkey: String,
    @SerializedName("account")
    val account: AccountInfo,
) : Parcelable

@Parcelize
data class AccountInfo(
    @SerializedName("data")
    val data: Data,
    @SerializedName("executable")
    val executable: Boolean,
    @SerializedName("lamports")
    val lamports: Long,
    @SerializedName("owner")
    val owner: String,
    @SerializedName("rentEpoch")
    val rentEpoch: BigInteger,
    @SerializedName("space")
    val space: Int,
) : Parcelable

@Parcelize
data class Data(
    @SerializedName("parsed")
    val parsed: Parsed,
    @SerializedName("program")
    val program: String,
    @SerializedName("space")
    val space: Int
) : Parcelable

@Parcelize
data class Parsed(
    @SerializedName("info")
    val info: Info,
    @SerializedName("type")
    val type: String
) : Parcelable

@Parcelize
data class Info(
    @SerializedName("meta")
    val meta: Meta,
    @SerializedName("stake")
    val stake: Stake
) : Parcelable

@Parcelize
data class Meta(
    @SerializedName("authorized")
    val authorized: Authorized,
    @SerializedName("lockup")
    val lockup: Lockup,
    @SerializedName("rentExemptReserve")
    val rentExemptReserve: String
) : Parcelable

@Parcelize
data class Authorized(
    @SerializedName("staker")
    val staker: String,
    @SerializedName("withdrawer")
    val withdrawer: String
) : Parcelable

@Parcelize
data class Lockup(
    @SerializedName("custodian")
    val custodian: String,
    @SerializedName("epoch")
    val epoch: Int,
    @SerializedName("unixTimestamp")
    val unixTimestamp: Int
) : Parcelable

@Parcelize
data class Stake(
    @SerializedName("creditsObserved")
    val creditsObserved: BigInteger,
    @SerializedName("delegation")
    val delegation: Delegation
) : Parcelable

@Parcelize
data class Delegation(
    @SerializedName("activationEpoch")
    val activationEpoch: String,
    @SerializedName("deactivationEpoch")
    val deactivationEpoch: String,
    @SerializedName("stake")
    val stake: String,
    @SerializedName("voter")
    val voter: String,
    @SerializedName("warmupCooldownRate")
    val warmupCooldownRate: Double
) : Parcelable
