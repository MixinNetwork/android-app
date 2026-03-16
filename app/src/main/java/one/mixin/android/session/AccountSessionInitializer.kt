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
import one.mixin.android.util.database.clearJobsAndRawTransaction
import one.mixin.android.vo.Account

suspend fun initializeAccountSession(
    context: Context,
    account: Account,
    sessionKey: EdKeyPair,
) {
    withContext(Dispatchers.IO) {
        clearJobsAndRawTransaction(context, account.identityNumber)
    }

    CryptoWalletHelper.clear(context)
    context.defaultSharedPreferences.clear()

    val privateKey = sessionKey.privateKey
    val pinToken = decryptPinToken(account.pinToken.decodeBase64(), privateKey)
    Session.storeEd25519Seed(privateKey.base64Encode())
    Session.storePinToken(pinToken.base64Encode())
    Session.storeAccount(account)
    resolveCurrentUserScopeManager(context).enter(account)

    if (Session.hasPhone()) {
        removeValueFromEncryptedPreferences(context, Constants.Tip.MNEMONIC)
    }

    context.defaultSharedPreferences.putString(DEVICE_ID, context.getStringDeviceId())
}
