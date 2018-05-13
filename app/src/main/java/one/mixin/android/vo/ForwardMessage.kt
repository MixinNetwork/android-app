package one.mixin.android.vo

import android.annotation.SuppressLint
import android.arch.persistence.room.Entity
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@SuppressLint("ParcelCreator")
@Entity
@Parcelize
data class ForwardMessage(
    val type: String,
    val content: String? = null,
    val mediaUrl: String? = null,
    val mediaName: String? = null,
    val mediaType: String? = null,
    val mediaSize: Long? = null,
    val userId: String? = null,
    val albumId: String? = null,
    val assetName: String? = null,
    val msgCreatedAt: String? = null,
    val sharedUserId: String? = null,
    val messageId: String? = null
) : Parcelable

fun ForwardMessage.addTo(list: MutableList<ForwardMessage>) {
    list.add(this)
}