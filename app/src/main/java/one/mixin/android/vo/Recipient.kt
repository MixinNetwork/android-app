package one.mixin.android.vo

import android.os.Parcelable
import android.view.View
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.room.ColumnInfo
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import one.mixin.android.R

interface Recipient

data class AddressItem(
    @ColumnInfo(name = "address_id")
    @SerializedName("address_id")
    val id: String,
    @ColumnInfo(name = "label")
    @SerializedName("label")
    val label: String,
    @ColumnInfo(name = "icon_url")
    @SerializedName("icon_url")
    val iconUrl: String,
    @ColumnInfo(name = "chain_icon_url")
    @SerializedName("chain_icon_url")
    val chainIconUrl: String,
    @ColumnInfo(name = "destination")
    @SerializedName("destination")
    val destination: String,
    @ColumnInfo(name = "tag")
    @SerializedName("tag")
    val tag: String?,
) : Recipient {
    companion object {
        val DIFF_CALLBACK =
            object : DiffUtil.ItemCallback<AddressItem>() {
                override fun areItemsTheSame(
                    oldItem: AddressItem,
                    newItem: AddressItem,
                ) =
                    oldItem.id == newItem.id

                override fun areContentsTheSame(
                    oldItem: AddressItem,
                    newItem: AddressItem,
                ) =
                    oldItem == newItem
            }
    }
}

@Parcelize
data class UserItem(
    @ColumnInfo(name = "user_id")
    val id: String,
    @ColumnInfo(name = "full_name")
    val fullName: String?,
    @ColumnInfo(name = "avatar_url")
    val avatarUrl: String?,
    @ColumnInfo(name = "identity_number")
    val identityNumber: String,
    @ColumnInfo(name = "is_verified")
    val isVerified: Boolean?,
    @ColumnInfo(name = "app_id")
    val appId: String?,
    @ColumnInfo(name = "membership")
    val membership: Membership?
) : Recipient, Parcelable {
    companion object {
        val DIFF_CALLBACK =
            object : DiffUtil.ItemCallback<UserItem>() {
                override fun areItemsTheSame(
                    oldItem: UserItem,
                    newItem: UserItem,
                ) =
                    oldItem.id == newItem.id

                override fun areContentsTheSame(
                    oldItem: UserItem,
                    newItem: UserItem,
                ) =
                    oldItem == newItem
            }
    }

    fun isMembership(): Boolean {
        return membership?.isMembership() == true
    }

}

fun UserItem.showVerifiedOrBot(
    verifiedView: View,
    botView: View,
    membershipIv: ImageView
) {
    when {
        isMembership() -> {
            verifiedView.isVisible = false
            botView.isVisible = false
            membershipIv.setImageResource(membership.membershipIcon())
            membershipIv.isVisible = true
        }
        isVerified == true -> {
            verifiedView.isVisible = true
            botView.isVisible = false
        }

        appId != null -> {
            verifiedView.isVisible = false
            botView.isVisible = true
        }

        else -> {
            verifiedView.isVisible = false
            botView.isVisible = false
        }
    }
}

fun AddressItem.displayAddress(): String {
    return if (!tag.isNullOrEmpty()) {
        "$destination:$tag"
    } else {
        destination
    }
}

fun AddressItem.formatAddress(): String {
    val address = displayAddress()
    return address.substring(0, 3) + "..." + address.substring(address.length - 3, address.length)
}

