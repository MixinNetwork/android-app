package one.mixin.android.util.backup.drive

interface DriveFile : DriveResource {
    companion object {
        const val MODE_READ_ONLY = 268435456
        const val MODE_READ_WRITE = 805306368
        const val MODE_WRITE_ONLY = 536870912
    }

    @Retention(AnnotationRetention.SOURCE)
    annotation class OpenMode
}