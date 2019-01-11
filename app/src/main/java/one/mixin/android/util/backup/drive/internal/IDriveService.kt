package one.mixin.android.util.backup.drive.internal

import android.content.Context
import android.os.Bundle
import android.os.IBinder
import android.os.Looper
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.internal.ClientSettings
import com.google.android.gms.common.internal.GmsClient

class IDriveService(
    context: Context,
    looper: Looper,
    clientSettings: ClientSettings,
    connectionCallbacks: GoogleApiClient.ConnectionCallbacks,
    onConnectionFailedListener: GoogleApiClient.OnConnectionFailedListener,
    bundle: Bundle
) : GmsClient<IDrive>(context, looper, 11, clientSettings, connectionCallbacks, onConnectionFailedListener) {
    private val packageName = clientSettings.realClientPackageName
    private val bundleData = bundle

    override fun createServiceInterface(p0: IBinder?): IDrive? {
        return null
    }

    override fun getServiceDescriptor() = "one.mixin.android.util.backup.drive.internal.IDriveService"

    override fun getStartServiceAction() = "com.google.android.gms.drive.ApiService.START"
}