package one.mixin.android.ui.tip.wc

import android.content.Context
import androidx.lifecycle.ViewModel
import com.reown.walletkit.client.Wallet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.mixin.android.api.request.web3.EstimateFeeRequest
import one.mixin.android.api.request.web3.Web3RawTransactionRequest
import one.mixin.android.api.response.web3.WalletOutput
import one.mixin.android.crypto.CryptoWalletHelper
import one.mixin.android.repository.TokenRepository
import one.mixin.android.repository.Web3Repository
import one.mixin.android.tip.Tip
import one.mixin.android.tip.wc.WalletConnect
import one.mixin.android.tip.wc.WalletConnectV2
import one.mixin.android.tip.wc.internal.Chain
import one.mixin.android.tip.wc.internal.WcBitcoinFeeEstimate
import one.mixin.android.tip.wc.internal.WcBitcoinSignedTransfer
import one.mixin.android.web3.js.Web3Signer
import org.sol4kt.VersionedTransactionCompat
import org.web3j.crypto.Hash
import timber.log.Timber
import java.util.Base64
import javax.inject.Inject

@HiltViewModel
class WalletConnectBottomSheetViewModel
    @Inject
    internal constructor(
        private val assetRepo: TokenRepository,
        private val web3Repository: Web3Repository,
        private val tip: Tip,
    ) : ViewModel() {

        suspend fun estimateFee(request: EstimateFeeRequest) = web3Repository.estimateFee(request)

        suspend fun getV2SessionProposal(topic: String): Wallet.Model.SessionProposal? {
            return withContext(Dispatchers.IO) {
                WalletConnectV2.getSessionProposal(topic)
            }
        }

        suspend fun getV2SessionRequest(topic: String): Wallet.Model.SessionRequest? {
            return withContext(Dispatchers.IO) {
                WalletConnectV2.getSessionRequest(topic)
            }
        }

        fun parseV2SignData(
            address: String,
            sessionRequest: Wallet.Model.SessionRequest,
        ): WalletConnect.WCSignData.V2SignData<*>? {
            return WalletConnectV2.parseSessionRequest(address, sessionRequest)
        }

        suspend fun getWeb3Priv(
            context: Context,
            pin: String,
            chainId: String,
        ): ByteArray {
            val result = tip.getOrRecoverTipPriv(context, pin)
            val spendKey = tip.getSpendPrivFromEncryptedSalt(tip.getMnemonicFromEncryptedPreferences(context), tip.getEncryptedSalt(context), pin, result.getOrThrow())
            return requireNotNull(CryptoWalletHelper.getWeb3PrivateKey(context, spendKey, chainId))
        }

        suspend fun verifyPin(
            context: Context,
            pin: String,
        ) {
            val result = tip.getOrRecoverTipPriv(context, pin)
            result.getOrThrow()
        }

        suspend fun refreshAsset(assetId: String) = assetRepo.refreshAsset(assetId)

        suspend fun outputsByAddress(
            address: String,
            assetId: String,
        ): List<WalletOutput> =
            withContext(Dispatchers.IO) {
                web3Repository.outputsByAddress(address, assetId)
            }

        suspend fun estimateBitcoinFee(rawTx: String): WcBitcoinFeeEstimate? =
            withContext(Dispatchers.IO) {
                val response =
                    web3Repository.estimateFee(
                        EstimateFeeRequest(
                            chainId = Chain.Bitcoin.assetId,
                            rawTransaction = rawTx,
                            data = null,
                        ),
                    )
                if (response.isSuccess && response.data != null) {
                    WcBitcoinFeeEstimate(
                        feeRate = response.data!!.feeRate?.toBigDecimalOrNull(),
                        minFee = response.data!!.minFee,
                    )
                } else {
                    null
                }
            }

        suspend fun sendTransaction(
            signedTransactionData: Any,
            chain: Chain,
            sessionRequest: Wallet.Model.SessionRequest,
            account: String,
            to: String?,
        ): String? {
            if (signedTransactionData is WcBitcoinSignedTransfer) {
                return sendBitcoinTransfer(signedTransactionData, sessionRequest)
            }
            val signature: String
            val rawTx = if (chain == Chain.Solana) {
                signedTransactionData as VersionedTransactionCompat
                signature = signedTransactionData.signatures.first()
                Base64.getEncoder().encodeToString(signedTransactionData.serialize())
            } else {
                signedTransactionData as String
                signature =  Hash.sha3(signedTransactionData)
                signedTransactionData
            }
            try {
                Timber.d("${WalletConnectV2.TAG} sendTransaction postRawTx start topic=${sessionRequest.topic} requestId=${sessionRequest.request.id} chain=${chain.chainId} account=$account to=$to txId=$signature")
                assetRepo.postRawTx(Web3RawTransactionRequest(chain.getWeb3ChainId(), rawTx, account, to))
            } catch (e: Exception) {
                Timber.d("${WalletConnectV2.TAG} sendTransaction postRawTx error topic=${sessionRequest.topic} requestId=${sessionRequest.request.id} chain=${chain.chainId} txId=$signature error=${e.message}")
                WalletConnectV2.rejectRequest(e.message, sessionRequest)
                throw e
            }
            Timber.d("${WalletConnectV2.TAG} sendTransaction postRawTx success topic=${sessionRequest.topic} requestId=${sessionRequest.request.id} chain=${chain.chainId} txId=$signature")
            if (chain == Chain.Solana) {
                WalletConnectV2.approveSolanaTransaction(signature, sessionRequest)
            } else {
                WalletConnectV2.approveRequestInternal(signature, sessionRequest)
            }
            return null
        }

        private suspend fun sendBitcoinTransfer(
            transfer: WcBitcoinSignedTransfer,
            sessionRequest: Wallet.Model.SessionRequest,
        ): String? {
            try {
                Timber.d("${WalletConnectV2.TAG} sendBitcoinTransfer postRawTx start topic=${sessionRequest.topic} requestId=${sessionRequest.request.id} account=${transfer.fromAddress} to=${transfer.recipientAddress}")
                val response =
                    assetRepo.postRawTx(
                        Web3RawTransactionRequest(
                            Chain.Bitcoin.assetId,
                            transfer.rawTx,
                            transfer.fromAddress,
                            transfer.recipientAddress,
                        ),
                        Chain.Bitcoin.assetId,
                        transfer.feeRate,
                    )
                if (!response.isSuccess || response.data == null) {
                    val message = response.error?.description ?: "post raw transaction failed"
                    throw IllegalArgumentException(message)
                }
                web3Repository.walletOutputDao.updateOutputsToSigned(transfer.consumedOutputIds)
                web3Repository.insertBitcoinChangeOutputs(transfer.fromAddress, transfer.rawTx)
                web3Repository.refreshBitcoinTokenAmount(Web3Signer.currentWalletId, transfer.fromAddress)
                WalletConnectV2.approveRequestInternal(response.data!!.hash, sessionRequest)
            } catch (e: Exception) {
                Timber.d("${WalletConnectV2.TAG} sendBitcoinTransfer postRawTx error topic=${sessionRequest.topic} requestId=${sessionRequest.request.id} error=${e.message}")
                WalletConnectV2.rejectRequest(e.message, sessionRequest)
                throw e
            }
            return null
        }
    }
