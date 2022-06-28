@file:Suppress("unused")

package one.mixin.android.tip

import one.mixin.android.extension.sha256

object TipBody {
    private const val TIPVerify = "TIP:VERIFY:"
    private const val TIPAddressAdd = "TIP:ADDRESS:ADD:"
    private const val TIPAddressRemove = "TIP:ADDRESS:REMOVE:"
    private const val TIPUserDeactivate = "TIP:USER:DEACTIVATE:"
    private const val TIPEmergencyContactCreate = "TIP:EMERGENCY:CONTACT:CREATE:"
    private const val TIPEmergencyContactRead = "TIP:EMERGENCY:CONTACT:READ:"
    private const val TIPEmergencyContactRemove = "TIP:EMERGENCY:CONTACT:REMOVE:"
    private const val TIPPhoneNumberUpdate = "TIP:PHONE:NUMBER:UPDATE:"
    private const val TIPMultisigRequestSign = "TIP:MULTISIG:REQUEST:SIGN:"
    private const val TIPMultisigRequestUnlock = "TIP:MULTISIG:REQUEST:UNLOCK:"
    private const val TIPCollectibleRequestSign = "TIP:COLLECTIBLE:REQUEST:SIGN:"
    private const val TIPCollectibleRequestUnlock = "TIP:COLLECTIBLE:REQUEST:UNLOCK:"
    private const val TIPTransferCreate = "TIP:TRANSFER:CREATE:"
    private const val TIPWithdrawalCreate = "TIP:WITHDRAWAL:CREATE:"
    private const val TIPRawTransactionCreate = "TIP:TRANSACTION:CREATE:"

    fun forVerify(timestamp: Long): ByteArray =
        "$TIPVerify${String.format("%032d", timestamp)}".toByteArray()

    fun forRawTransactionCreate(assetId :String, opponentKey: String, opponentReceivers: List<String>, opponentThreshold: Long, amount: Double, traceId: String, memo: String): ByteArray {
        var body = assetId + opponentKey
        opponentReceivers.forEach { o -> body += o }
        body = body + opponentThreshold + amount + traceId + memo
        return (TIPTransferCreate + body).hashToBody()
    }

    fun forWithdrawalCreate(addressId: String, amount: String, fee: String?, traceId: String, memo: String?): ByteArray =
        (TIPWithdrawalCreate + addressId + amount + fee + traceId + memo).hashToBody()

    fun forTransfer(assetId: String, counterUserId: String, amount: String, traceId: String?, memo: String?): ByteArray =
        (TIPTransferCreate + assetId + counterUserId + amount + traceId + memo).hashToBody()

    fun forPhoneNumberUpdate(verificationId: String, code: String): ByteArray =
        (TIPPhoneNumberUpdate + verificationId + code).hashToBody()

    fun forEmergencyContactCreate(verificationId: String, code: String): ByteArray =
        (TIPEmergencyContactCreate + verificationId + code).hashToBody()

    fun forAddressAdd(assetId: String, publicKey: String?, keyTag: String?, name: String?): ByteArray =
        (TIPAddressAdd + assetId + publicKey + keyTag + name).hashToBody()

    private fun String.hashToBody() = sha256()
}