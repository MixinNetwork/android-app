package one.mixin.android.util.backup.drive

import android.annotation.SuppressLint
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@SuppressLint("ParcelCreator")
@Parcelize
data class DriveId(
    val resourceId: String,
    val resourceType: Int = RESOURCE_TYPE_UNKNOWN
) : Parcelable {

    companion object {
        const val RESOURCE_TYPE_UNKNOWN = -1
        const val RESOURCE_TYPE_FILE = 0
        const val RESOURCE_TYPE_FOLDER = 1
    }
}