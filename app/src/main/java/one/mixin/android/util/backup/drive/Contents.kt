package one.mixin.android.util.backup.drive

import android.os.ParcelFileDescriptor
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.io.FileInputStream
import java.io.FileOutputStream

@Parcelize
data class Contents(
    val parcelFileDescriptor: ParcelFileDescriptor,
    val requestId: Int,
    val mode: Int,
    val driveId: DriveId
) : Parcelable {
    fun getInputStream() = FileInputStream(parcelFileDescriptor.fileDescriptor)

    fun getOutputStream() = FileOutputStream(parcelFileDescriptor.fileDescriptor)
}