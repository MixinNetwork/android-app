package one.mixin.android.session

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.Constants.DEVICE_ID
import one.mixin.android.crypto.CryptoWalletHelper
import one.mixin.android.crypto.EdKeyPair
import one.mixin.android.crypto.removeValueFromEncryptedPreferences
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.clear
import one.mixin.android.extension.decodeBase64
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getStringDeviceId
import one.mixin.android.extension.putString
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.util.database.clearJobsAndRawTransaction
import one.mixin.android.vo.Account

suspend fun initializeAccountSession(
    context: Context,
    account: Account,
    sessionKey: EdKeyPair,
) {
    // Check if we're logging into the same account to preserve user settings
    val isSameUser = Session.getAccountId() == account.userId

    // Store session data first
    val privateKey = sessionKey.privateKey
    val pinToken = decryptPinToken(account.pinToken.decodeBase64(), privateKey)
    Session.storeEd25519Seed(privateKey.base64Encode())
    Session.storePinToken(pinToken.base64Encode())
    Session.storeAccount(account)
    AnalyticsTracker.setAppsFlyerCustomerUserId(account)

    // Enter the user scope and migrate databases BEFORE clearing anything
    // This ensures we operate on the correct scoped database after migration
    resolveCurrentUserScopeManager(context).enter(account)

    // Now clear jobs and raw transactions from the migrated scoped database
    withContext(Dispatchers.IO) {
        clearJobsAndRawTransaction(context, account.identityNumber)
    }

    // Now clear any sensitive data
    CryptoWalletHelper.clear(context)
    if (!isSameUser) {
        context.defaultSharedPreferences.clear()
    }

    if (Session.hasPhone()) {
        removeValueFromEncryptedPreferences(context, Constants.Tip.MNEMONIC)
    }

    context.defaultSharedPreferences.putString(DEVICE_ID, context.getStringDeviceId())
}
