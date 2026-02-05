package one.mixin.android.ui.tip

import android.content.Context
import android.security.keystore.UserNotAuthenticatedException
import dagger.hilt.android.scopes.ActivityRetainedScoped
import one.mixin.android.Constants
import one.mixin.android.Constants.Account.ChainAddress.EVM_ADDRESS
import one.mixin.android.Constants.Account.ChainAddress.SOLANA_ADDRESS
import one.mixin.android.Constants.Account.ChainAddress.BTC_ADDRESS
import one.mixin.android.Constants.ChainId.ETHEREUM_CHAIN_ID
import one.mixin.android.Constants.ChainId.SOLANA_CHAIN_ID
import one.mixin.android.Constants.INTERVAL_10_MINS
import one.mixin.android.R
import one.mixin.android.api.request.RegisterRequest
import one.mixin.android.api.request.web3.Web3AddressRequest
import one.mixin.android.api.request.web3.WalletRequest
import one.mixin.android.api.service.AccountService
import one.mixin.android.api.service.UtxoService
import one.mixin.android.crypto.PinCipher
import one.mixin.android.crypto.PrivacyPreference.putPrefPinInterval
import one.mixin.android.crypto.initFromSeedAndSign
import one.mixin.android.crypto.newKeyPairFromSeed
import one.mixin.android.crypto.removeValueFromEncryptedPreferences
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.hexString
import one.mixin.android.extension.putLong
import one.mixin.android.extension.toHex
import one.mixin.android.repository.Web3Repository
import one.mixin.android.session.Session
import one.mixin.android.tip.Tip
import one.mixin.android.tip.TipBody
import one.mixin.android.tip.bip44.Bip44Path
import one.mixin.android.tip.getTipExceptionMsg
import one.mixin.android.tip.privateKeyToAddress
import one.mixin.android.tip.tipPrivToPrivateKey
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.ui.wallet.fiatmoney.requestRouteAPI
import one.mixin.android.util.BiometricUtil
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.getMixinErrorStringByCode
import one.mixin.android.db.web3.vo.Web3Wallet
import one.mixin.android.vo.WalletCategory
import one.mixin.android.web3.js.JsSignMessage
import one.mixin.android.web3.js.Web3Signer
import org.web3j.utils.Numeric
import java.time.Instant
import javax.inject.Inject

@ActivityRetainedScoped
class TipCreateInteractor @Inject internal constructor(
    private val tip: Tip,
    private val accountService: AccountService,
    private val web3Repository: Web3Repository,
    private val utxoService: UtxoService,
    private val pinCipher: PinCipher,
) {
    suspend fun executeCreate(
        context: Context,
        pin: String,
        shouldOpenMainActivity: Boolean,
        onStepChanged: (TipStep) -> Unit,
        onShowMessage: (String) -> Unit,
    ): Boolean {
        val deviceId: String = requireNotNull(context.defaultSharedPreferences.getString(one.mixin.android.Constants.DEVICE_ID, null)) { "required deviceId can not be null" }
        val tipCounter = Session.getTipCounter()
        if (tipCounter >= 1) {
            onShowMessage("tip create only: tipCounter=$tipCounter")
            return false
        }
        var nodeFailedInfo = ""
        val observer: Tip.Observer = object : Tip.Observer {
            override fun onSyncing(step: Int, total: Int) {
                onStepChanged(Processing.SyncingNode(step, total))
            }
            override fun onSyncingComplete() {
                onStepChanged(Processing.Updating)
            }
            override fun onNodeFailed(info: String) {
                nodeFailedInfo = info
            }
        }
        onStepChanged(Processing.Creating)
        tip.addObserver(observer)
        val tipResult: Result<ByteArray?> = try {
            tip.createTipPriv(context, pin, deviceId, null, null)
        } finally {
            tip.removeObserver(observer)
        }
        val tipPriv: ByteArray? = tipResult.getOrElse { e: Throwable ->
            val errMsg = e.getTipExceptionMsg(context, nodeFailedInfo)
            onShowMessage(errMsg)
            return false
        }
        val registerSuccess: Boolean = if (!Session.hasSafe()) {
            try {
                registerPublicKey(context, pin, tipPriv, onStepChanged, onShowMessage)
            } catch (e: Exception) {
                val errorInfo = e.getTipExceptionMsg(context)
                onShowMessage(errorInfo)
                false
            }
        } else {
            true
        }
        if (!registerSuccess) {
            return false
        }
        val cur = System.currentTimeMillis()
        context.defaultSharedPreferences.putLong(Constants.Account.PREF_PIN_CHECK, cur)
        putPrefPinInterval(context, INTERVAL_10_MINS)
        val openBiometrics = context.defaultSharedPreferences.getBoolean(Constants.Account.PREF_BIOMETRICS, false)
        if (openBiometrics) {
            try {
                BiometricUtil.savePin(context, pin)
            } catch (ignored: UserNotAuthenticatedException) {
                onShowMessage(ignored.toString())
            }
        }
        if (shouldOpenMainActivity) {
            MainActivity.show(context)
        }
        return true
    }

    private suspend fun registerPublicKey(
        context: Context,
        pin: String,
        tipPriv: ByteArray?,
        onStepChanged: (TipStep) -> Unit,
        onShowMessage: (String) -> Unit,
    ): Boolean {
        onStepChanged(Processing.Registering)
        val meResp = accountService.getMeSuspend()
        if (meResp.isSuccess) {
            val account = requireNotNull(meResp.data) { "required account can not be null" }
            Session.storeAccount(account)
            if (account.hasSafe) {
                return true
            }
        } else {
            val error = requireNotNull(meResp.error) { "error can not be null" }
            val errorInfo = context.getMixinErrorStringByCode(error.code, error.description)
            onShowMessage(errorInfo)
            return false
        }
        val seed = try {
            tipPriv ?: tip.getOrRecoverTipPriv(context, pin).getOrThrow()
        } catch (e: Exception) {
            val errorInfo = e.getTipExceptionMsg(context)
            onShowMessage(errorInfo)
            return false
        }
        val spendSeed = tip.getSpendPriv(context, seed)
        val saltBase64 = tip.getEncryptSalt(context, pin, seed, Session.isAnonymous())
        val spendKeyPair = newKeyPairFromSeed(spendSeed)
        val selfAccountId = requireNotNull(Session.getAccountId()) { "self userId can not be null at this step" }
        val edKey = tip.getMnemonicEdKey(context, pin, seed)
        val pkHex: String = spendKeyPair.publicKey.toHex()
        val registerRequest = RegisterRequest(
            publicKey = pkHex,
            signature = Session.getRegisterSignature(selfAccountId, spendSeed),
            pin = getEncryptedTipBody(selfAccountId, pkHex, pin),
            salt = saltBase64,
            masterPublicHex = edKey.publicKey.hexString(),
            masterSignatureHex = initFromSeedAndSign(edKey.privateKey.toTypedArray().toByteArray(), selfAccountId.toByteArray()).hexString(),
        )
        val registerResp = utxoService.registerPublicKey(registerRequest)
        if (registerResp.isSuccess) {
            val solAddress: String = getTipAddress(context, pin, SOLANA_CHAIN_ID)
            PropertyHelper.updateKeyValue(SOLANA_ADDRESS, solAddress)
            val evmAddress: String = getTipAddress(context, pin, ETHEREUM_CHAIN_ID)
            PropertyHelper.updateKeyValue(EVM_ADDRESS, evmAddress)
            val btcAddress: String = getTipAddress(context, pin, Constants.ChainId.BITCOIN_CHAIN_ID)
            PropertyHelper.updateKeyValue(BTC_ADDRESS, btcAddress)
            Web3Signer.updateAddress(Web3Signer.JsSignerNetwork.Solana.name, solAddress)
            Web3Signer.updateAddress(Web3Signer.JsSignerNetwork.Ethereum.name, evmAddress)
            Session.storeAccount(requireNotNull(registerResp.data) { "required account can not be null" })
            createWallet(context, spendSeed)
            if (Session.hasPhone()) {
                removeValueFromEncryptedPreferences(context, Constants.Tip.MNEMONIC)
            }
            return true
        }
        if (registerResp.errorCode == ErrorHandler.INVALID_PIN_FORMAT) {
            onShowMessage(context.getString(R.string.error_legacy_pin))
            return false
        }
        val error = requireNotNull(registerResp.error) { "error can not be null" }
        val errorInfo = context.getMixinErrorStringByCode(error.code, error.description)
        onShowMessage(errorInfo)
        return false
    }

    private suspend fun getEncryptedTipBody(
        userId: String,
        pkHex: String,
        pin: String,
    ): String = pinCipher.encryptPin(pin, TipBody.forSequencerRegister(userId, pkHex))

    private suspend fun getTipAddress(
        context: Context,
        pin: String,
        chainId: String,
    ): String {
        val result = tip.getOrRecoverTipPriv(context, pin)
        val spendKey = tip.getSpendPrivFromEncryptedSalt(
            tip.getMnemonicFromEncryptedPreferences(context),
            tip.getEncryptedSalt(context),
            pin,
            result.getOrThrow(),
        )
        return privateKeyToAddress(spendKey, chainId)
    }

    private suspend fun createWallet(context: Context, spendKey: ByteArray) {
        val hasClassicWallet: Boolean = web3Repository.getClassicWalletId() != null
        if (hasClassicWallet) {
            return
        }
        val walletName: String = context.getString(R.string.Common_Wallet)
        val classicIndex = 0
        val evmAddress: String = privateKeyToAddress(spendKey, ETHEREUM_CHAIN_ID, classicIndex)
        val solAddress: String = privateKeyToAddress(spendKey, SOLANA_CHAIN_ID, classicIndex)
        val btcAddress: String = privateKeyToAddress(spendKey, Constants.ChainId.BITCOIN_CHAIN_ID, classicIndex)
        val addresses: List<Web3AddressRequest> = listOf(
            createSignedWeb3AddressRequest(destination = evmAddress, chainId = ETHEREUM_CHAIN_ID, path = Bip44Path.ethereumPathString(classicIndex), privateKey = tipPrivToPrivateKey(spendKey, ETHEREUM_CHAIN_ID, classicIndex), category = WalletCategory.CLASSIC.value),
            createSignedWeb3AddressRequest(destination = solAddress, chainId = SOLANA_CHAIN_ID, path = Bip44Path.solanaPathString(classicIndex), privateKey = tipPrivToPrivateKey(spendKey, SOLANA_CHAIN_ID, classicIndex), category = WalletCategory.CLASSIC.value),
            createSignedWeb3AddressRequest(destination = btcAddress, chainId = Constants.ChainId.BITCOIN_CHAIN_ID, path = Bip44Path.bitcoinSegwitPathString(classicIndex), privateKey = tipPrivToPrivateKey(spendKey, Constants.ChainId.BITCOIN_CHAIN_ID, classicIndex), category = WalletCategory.CLASSIC.value),
        )
        val walletRequest = WalletRequest(name = walletName, category = WalletCategory.CLASSIC.value, addresses = addresses)
        requestRouteAPI(
            invokeNetwork = { web3Repository.createWallet(walletRequest) },
            successBlock = { response ->
                response.data?.let { wallet ->
                    web3Repository.insertWallet(Web3Wallet(id = wallet.id, name = wallet.name, category = wallet.category, createdAt = wallet.createdAt, updatedAt = wallet.updatedAt))
                    val walletAddresses = wallet.addresses ?: emptyList()
                    if (walletAddresses.isNotEmpty()) {
                        web3Repository.insertAddressList(walletAddresses)
                    }
                }
            },
            requestSession = { ids ->
                web3Repository.fetchSessionsSuspend(ids)
            },
        )
    }

    private fun createSignedWeb3AddressRequest(
        destination: String,
        chainId: String,
        path: String?,
        privateKey: ByteArray,
        category: String,
    ): Web3AddressRequest {
        val selfId = Session.getAccountId()
        if (category == WalletCategory.WATCH_ADDRESS.value) {
            return Web3AddressRequest(destination = destination, chainId = chainId, path = path)
        }
        val now = Instant.now()
        val message = "$destination\n$selfId\n${now.epochSecond}"
        val signature = if (chainId == SOLANA_CHAIN_ID) {
            Numeric.prependHexPrefix(Web3Signer.signSolanaMessage(privateKey, message.toByteArray()))
        } else if (chainId in Constants.Web3EvmChainIds) {
            Web3Signer.signEthMessage(privateKey, message.toByteArray().toHexString(), JsSignMessage.TYPE_PERSONAL_MESSAGE)
        } else {
            null
        }
        return Web3AddressRequest(destination = destination, chainId = chainId, path = path, signature = signature, timestamp = now.toString())
    }
}
