//package one.mixin.android.util.backup.drive
//
//import android.content.Context
//import com.google.android.gms.tasks.Task
//import one.mixin.android.util.backup.drive.internal.AppFolderTaskApiCall
//
//class DriveResourceClientImpl(
//    context: Context,
//    driveSignInAccountOptions: DriveSignInAccountOptions
//) : DriveResourceClient(context, driveSignInAccountOptions) {
//    override fun getAppFolder(): Task<DriveFolder> {
//        return doRead(AppFolderTaskApiCall())
//    }
//
//    override fun createContents(): Task<DriveContents> {
//    }
//
//    override fun createFile(driveFolder: DriveFolder): Task<DriveFile> {
//    }
//}