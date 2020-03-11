package one.mixin.android.vo

import android.annotation.SuppressLint
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@SuppressLint("ParcelCreator")
@Parcelize
data class Location(
    val latitude: Double,
    val longitude: Double,
    val name: String?,
    val address: String?
) : Parcelable
