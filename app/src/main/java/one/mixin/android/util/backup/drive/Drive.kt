package one.mixin.android.util.backup.drive

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.Api
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes

class Drive {

    companion object {
        val SCOPE_FILE = Scope(DriveScopes.DRIVE_FILE)

        lateinit var driveApi: Api<DriveSignInAccountOptions>

        fun getDriveResourceClient(context: Context, account: GoogleSignInAccount): DriveResourceClient {
            return DriveResourceClientImpl(
                context,
                DriveSignInAccountOptions(account)
            )
        }
    }
}