package one.mixin.android.util.backup.drive

import android.os.ParcelFileDescriptor
import java.io.InputStream
import java.io.OutputStream

interface DriveContents {
    fun getDriveId(): DriveId

    fun getMode(): Int

    fun getParcelFileDescriptor(): ParcelFileDescriptor

    fun getInputStream(): InputStream

    fun getOutputStream(): OutputStream

    fun getContents(): Contents
}