package one.mixin.android.util.backup.drive

import android.content.Context
import com.google.android.gms.common.api.GoogleApi
import com.google.android.gms.drive.query.Query
import com.google.android.gms.tasks.Task

abstract class DriveResourceClient(
    context: Context,
    driveSignInAccountOptions: DriveSignInAccountOptions
) : GoogleApi<DriveSignInAccountOptions>(
    context,
    Drive.driveApi,
    driveSignInAccountOptions,
    Settings.DEFAULT_SETTINGS
) {
    abstract fun getAppFolder(): Task<DriveFolder>

    abstract fun createContents(): Task<DriveContents>

    abstract fun createFile(driveFolder: DriveFolder): Task<DriveFile>

    abstract fun delete(driveResource: DriveResource): Task<Void>
}