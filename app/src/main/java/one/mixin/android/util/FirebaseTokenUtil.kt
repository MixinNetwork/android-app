package one.mixin.android.util

import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import timber.log.Timber

private const val FIS_AUTH_ERROR = "FIS_AUTH_ERROR"

suspend fun retrieveFirebaseMessagingToken(): String =
    try {
        FirebaseMessaging.getInstance().token.await()
    } catch (e: Exception) {
        if (!e.isFisAuthError()) throw e
        Timber.w(e, "Firebase token retrieval failed with FIS_AUTH_ERROR")
        try {
            Timber.w("Deleting Firebase installation before retrying token retrieval")
            FirebaseInstallations.getInstance().delete().await()
            Timber.w("Firebase installation deleted, retrying token retrieval")
        } catch (deleteError: Exception) {
            Timber.e(deleteError, "Failed to delete Firebase installation after FIS_AUTH_ERROR")
            reportException("Failed to delete Firebase installation after FIS_AUTH_ERROR", deleteError)
            throw deleteError
        }
        FirebaseMessaging.getInstance().token.await()
    }

private fun Throwable.isFisAuthError(): Boolean =
    generateSequence(this) { it.cause }.any { throwable ->
        throwable.message?.contains(FIS_AUTH_ERROR, ignoreCase = true) == true
    }
