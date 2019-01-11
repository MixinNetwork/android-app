package one.mixin.android.util.backup.drive

interface DriveFolder : DriveResource {
    companion object {
        const val MIME_TYPE = "application/vnd.google-apps.folder"
    }
}