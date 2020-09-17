package one.mixin.android.webrtc

import android.os.Parcel
import android.os.Parcelable

data class SelectItem(val conversationId: String?, val userId: String?) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(conversationId)
        parcel.writeString(userId)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SelectItem> {
        override fun createFromParcel(parcel: Parcel): SelectItem {
            return SelectItem(parcel)
        }

        override fun newArray(size: Int): Array<SelectItem?> {
            return arrayOfNulls(size)
        }
    }
}
