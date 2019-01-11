package one.mixin.android.util.backup.drive

import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.Api

class DriveSignInAccountOptions(
    private val googleSignInAccount: GoogleSignInAccount
) : Api.ApiOptions.HasGoogleSignInAccountOptions {

    override fun getGoogleSignInAccount(): GoogleSignInAccount {
        return this.googleSignInAccount
    }

}