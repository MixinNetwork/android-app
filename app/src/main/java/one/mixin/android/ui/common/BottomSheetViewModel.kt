package one.mixin.android.ui.common

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kernel.Kernel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.MixinApplication
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.AccountUpdateRequest
import one.mixin.android.api.request.AddressRequest
import one.mixin.android.api.request.CollectibleRequest
import one.mixin.android.api.request.ConversationCircleRequest
import one.mixin.android.api.request.ConversationRequest
import one.mixin.android.api.request.DeactivateRequest
import one.mixin.android.api.request.ParticipantRequest
import one.mixin.android.api.request.PinRequest
import one.mixin.android.api.request.RawTransactionsRequest
import one.mixin.android.api.request.RegisterRequest
import one.mixin.android.api.request.RelationshipRequest
import one.mixin.android.api.request.TransactionRequest
import one.mixin.android.api.request.TransferRequest
import one.mixin.android.api.request.buildGhostKeyRequest
import one.mixin.android.api.request.buildKernelTransferGhostKeyRequest
import one.mixin.android.api.request.buildWithdrawalFeeGhostKeyRequest
import one.mixin.android.api.request.buildWithdrawalSubmitGhostKeyRequest
import one.mixin.android.api.response.AuthorizationResponse
import one.mixin.android.api.response.ConversationResponse
import one.mixin.android.api.response.TransactionResponse
import one.mixin.android.api.response.getTransactionResult
import one.mixin.android.api.service.UtxoService
import one.mixin.android.crypto.PinCipher
import one.mixin.android.db.MixinDatabase
import one.mixin.android.extension.escapeSql
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.toHex
import one.mixin.android.job.CheckBalanceJob
import one.mixin.android.job.ConversationJob
import one.mixin.android.job.GenerateAvatarJob
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshAccountJob
import one.mixin.android.job.RefreshAssetsJob
import one.mixin.android.job.RefreshConversationJob
import one.mixin.android.job.RefreshUserJob
import one.mixin.android.job.SyncOutputJob
import one.mixin.android.job.UpdateRelationshipJob
import one.mixin.android.net.executeWithRetry
import one.mixin.android.repository.AccountRepository
import one.mixin.android.repository.ConversationRepository
import one.mixin.android.repository.TokenRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.session.Session
import one.mixin.android.tip.Tip
import one.mixin.android.tip.TipBody
import one.mixin.android.ui.common.biometric.EmptyUtxoException
import one.mixin.android.ui.common.biometric.MaxCountNotEnoughUtxoException
import one.mixin.android.ui.common.biometric.NotEnoughUtxoException
import one.mixin.android.ui.common.biometric.SafeMultisigsBiometricItem
import one.mixin.android.ui.common.biometric.maxUtxoCount
import one.mixin.android.ui.common.message.CleanMessageHelper
import one.mixin.android.ui.common.message.SendMessageHelper
import one.mixin.android.ui.wallet.alert.vo.CoinItem
import one.mixin.android.util.SINGLE_DB_THREAD
import one.mixin.android.util.reportException
import one.mixin.android.util.uniqueObjectId
import one.mixin.android.vo.Account
import one.mixin.android.vo.Address
import one.mixin.android.vo.AddressItem
import one.mixin.android.vo.AssetPrecision
import one.mixin.android.vo.Circle
import one.mixin.android.vo.CircleConversation
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.ConversationCircleManagerItem
import one.mixin.android.vo.ConversationStatus
import one.mixin.android.vo.EncryptCategory
import one.mixin.android.vo.InscriptionCollection
import one.mixin.android.vo.InscriptionItem
import one.mixin.android.vo.MixinInvoice
import one.mixin.android.vo.Participant
import one.mixin.android.vo.Reference
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.Trace
import one.mixin.android.vo.User
import one.mixin.android.vo.UserItem
import one.mixin.android.vo.VerifiedTransactionData
import one.mixin.android.vo.assetIdToAsset
import one.mixin.android.vo.createConversation
import one.mixin.android.vo.generateConversationId
import one.mixin.android.vo.giphy.Gif
import one.mixin.android.vo.market.MarketItem
import one.mixin.android.vo.notMessengerUser
import one.mixin.android.vo.safe.Output
import one.mixin.android.vo.safe.OutputState
import one.mixin.android.vo.safe.RawTransaction
import one.mixin.android.vo.safe.RawTransactionType
import one.mixin.android.vo.safe.SafeSnapshot
import one.mixin.android.vo.safe.SafeSnapshotType
import one.mixin.android.vo.safe.SafeWithdrawal
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.vo.safe.UtxoWrapper
import one.mixin.android.vo.safe.formatDestination
import one.mixin.android.vo.toSimpleChat
import one.mixin.android.vo.utxo.SignResult
import one.mixin.android.vo.utxo.SignedTransaction
import one.mixin.android.vo.utxo.changeToOutput
import one.mixin.android.vo.utxo.consolidationOutput
import timber.log.Timber
import java.io.File
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class BottomSheetViewModel
    @Inject
    internal constructor(
        private val appDatabase: MixinDatabase,
        private val accountRepository: AccountRepository,
        private val jobManager: MixinJobManager,
        private val userRepository: UserRepository,
        private val tokenRepository: TokenRepository,
        private val conversationRepo: ConversationRepository,
        private val cleanMessageHelper: CleanMessageHelper,
        private val pinCipher: PinCipher,
        private val tip: Tip,
        private val utxoService: UtxoService,
        private val messenger: SendMessageHelper,
    ) : ViewModel() {
        suspend fun searchCode(code: String) =
            withContext(Dispatchers.IO) {
                accountRepository.searchCode(code)
            }

        fun join(code: String): Observable<MixinResponse<ConversationResponse>> =
            accountRepository.join(code).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

        fun refreshConversation(conversationId: String) {
            jobManager.addJobInBackground(RefreshConversationJob(conversationId))
        }

        suspend fun simpleAssetsWithBalance() =
            withContext(Dispatchers.IO) {
                tokenRepository.simpleAssetsWithBalance()
            }

        fun assetItems(): LiveData<List<TokenItem>> = tokenRepository.assetItems()

        fun coinItems(): LiveData<List<CoinItem>> = tokenRepository.coinItems()

        fun allRecipients(): LiveData<List<UserItem>> = userRepository.allRecipients()

        fun allAddresses(): LiveData<List<AddressItem>> = tokenRepository.allAddresses()

        fun assetItems(assetIds: List<String>): LiveData<List<TokenItem>> = tokenRepository.assetItems(assetIds)

        suspend fun findTokenItems(ids: List<String>): List<TokenItem> = tokenRepository.findTokenItems(ids)

        suspend fun findAssetItemsWithBalance(): List<TokenItem> = tokenRepository.findAssetItemsWithBalance()

        fun assetItemsWithBalance(): LiveData<List<TokenItem>> = tokenRepository.assetItemsWithBalance()

        suspend fun kernelWithdrawalTransaction(
            receiverId: String,
            traceId: String,
            assetId: String,
            feeAssetId: String,
            amount: String,
            feeAmount: String,
            destination: String,
            tag: String?,
            memo: String?,
            pin: String,
        ): MixinResponse<*> {
            val isDifferentFee = feeAssetId != assetId
            val asset = assetIdToAsset(assetId)
            val feeAsset = assetIdToAsset(feeAssetId)
            val senderId = Session.getAccountId()!!
            val threshold = 1.toByte()
            val feeTraceId = uniqueObjectId(traceId, "FEE")

            val withdrawalUtxos =
                UtxoWrapper(
                    packUtxo(
                        asset,
                        if (isDifferentFee) {
                            amount
                        } else {
                            (BigDecimal(amount) + BigDecimal(feeAmount)).toPlainString()
                        },
                    ),
                )
            val feeUtxos =
                if (isDifferentFee) {
                    UtxoWrapper(
                        packUtxo(
                            feeAsset,
                            feeAmount,
                        ),
                    )
                } else {
                    null
                }

            val context = MixinApplication.appContext
            val tipPriv = tip.getOrRecoverTipPriv(context, pin).getOrThrow()
            val spendKey = tip.getSpendPrivFromEncryptedSalt(tip.getMnemonicFromEncryptedPreferences(context), tip.getEncryptedSalt(context), pin, tipPriv)
            Timber.e("Kernel Withdrawal($traceId): begin")
            Timber.e("Kernel Withdrawal($traceId): request ghost key")
            val ghostKeyResponse =
                tokenRepository.ghostKey(
                    if (isDifferentFee) {
                        buildWithdrawalFeeGhostKeyRequest(receiverId, senderId, traceId)
                    } else {
                        buildWithdrawalSubmitGhostKeyRequest(receiverId, senderId, traceId)
                    },
                )
            if (ghostKeyResponse.error != null) {
                Timber.e("Kernel Withdrawal($traceId): request ghost key ${ghostKeyResponse.errorDescription}")
                return ghostKeyResponse
            }
            val data = ghostKeyResponse.data!!

            val feeOutputKeys = data[0].keys.joinToString(",")
            val feeOutputMask = data[0].mask

            val changeKeys = data[1].keys.joinToString(",")
            val changeMask = data[1].mask

            val withdrawalTx = Kernel.buildWithdrawalTx(asset, amount, destination, tag ?: "", if (isDifferentFee) "" else feeAmount, if (isDifferentFee) "" else feeOutputKeys, if (isDifferentFee) "" else feeOutputMask, withdrawalUtxos.input, changeKeys, changeMask, memo)
            val withdrawalRequests = mutableListOf(TransactionRequest(withdrawalTx.raw, traceId))

            val feeTx =
                if (isDifferentFee) {
                    val feeChangeKeys = data[2].keys.joinToString(",")
                    val feeChangeMask = data[2].mask
                    val feeTx = Kernel.buildTx(feeAsset, feeAmount, threshold.toInt(), feeOutputKeys, feeOutputMask, feeUtxos!!.input, feeChangeKeys, feeChangeMask, memo, withdrawalTx.hash)
                    withdrawalRequests.add(TransactionRequest(feeTx.raw, feeTraceId))
                    feeTx
                } else {
                    null
                }
            Timber.e("Kernel Withdrawal($traceId): request transaction")
            val withdrawalRequestResponse = tokenRepository.transactionRequest(withdrawalRequests)
            if (withdrawalRequestResponse.error != null) {
                Timber.e("Kernel Withdrawal($traceId): request transaction ${withdrawalRequestResponse.errorDescription}")
                return withdrawalRequestResponse
            } else if ((withdrawalRequestResponse.data?.size ?: 0) < 1) {
                Timber.e("Kernel Withdrawal($traceId): request transaction, Parameter exception")
                throw IllegalArgumentException("Parameter exception")
            } else if (withdrawalRequestResponse.data?.first()?.state != OutputState.unspent.name) {
                Timber.e("Kernel Withdrawal($traceId): request transaction, Transfer is already paid")
                throw IllegalArgumentException("Transfer is already paid")
            }
            val (withdrawalData, feeData) = getTransactionResult(withdrawalRequestResponse.data, traceId, feeTraceId)
            val withdrawalViews = withdrawalData.views!!.joinToString(",")
            val signWithdrawal = Kernel.signTx(withdrawalTx.raw, withdrawalUtxos.formatKeys, withdrawalViews, spendKey.toHex(), isDifferentFee)
            val signWithdrawalResult = SignResult(signWithdrawal.raw, signWithdrawal.change)
            val rawRequest = mutableListOf(TransactionRequest(signWithdrawalResult.raw, traceId))
            withContext(SINGLE_DB_THREAD) {
                if (isDifferentFee) {
                    feeUtxos ?: throw NullPointerException("Lost fee UTXO")
                    feeData ?: throw NullPointerException("Lost fee fee data")
                    val feeViews = feeData.views!!.joinToString(",")
                    val signFee = Kernel.signTx(feeTx!!.raw, feeUtxos.formatKeys, feeViews, spendKey.toHex(), false)
                    val signFeeResult = SignResult(signFee.raw, signFee.change)
                    rawRequest.add(TransactionRequest(signFeeResult.raw, feeTraceId))
                    Timber.e("Kernel Withdrawal($traceId): db begin")
                    appDatabase.runInTransaction {
                        Timber.e("Kernel Withdrawal($traceId): db update fee Utxo ${feeUtxos.ids}")
                        tokenRepository.updateUtxoToSigned(feeUtxos.ids)
                        Timber.e("Kernel Withdrawal($traceId): db update Utxo ${withdrawalUtxos.ids}")
                        tokenRepository.updateUtxoToSigned(withdrawalUtxos.ids)
                        if (signWithdrawalResult.change != null) {
                            val changeOutput = changeToOutput(signWithdrawalResult.change, asset, changeMask, data[1].keys, withdrawalUtxos.lastOutput)
                            Timber.e("Kernel Withdrawal($traceId): db insert change")
                            tokenRepository.insertOutput(changeOutput)
                        }
                        if (signFeeResult.change != null) {
                            val changeOutput = changeToOutput(signFeeResult.change, feeAsset, changeMask, data[2].keys, feeUtxos.lastOutput)
                            Timber.e("Kernel Withdrawal($traceId): db insert fee change")
                            tokenRepository.insertOutput(changeOutput)
                        }
                        val transactionHash = signWithdrawal.hash
                        Timber.e("Kernel Withdrawal($traceId): db insert snapshot")
                        tokenRepository.insertSafeSnapshot(
                            UUID.nameUUIDFromBytes("$senderId:$transactionHash".toByteArray()).toString(),
                            senderId,
                            "",
                            transactionHash,
                            traceId,
                            assetId,
                            amount,
                            memo,
                            SafeSnapshotType.withdrawal,
                            withdrawal =
                                SafeWithdrawal(
                                    "",
                                    destination,
                                ),
                        )
                        val feeTransactionHash = signFee.hash
                        Timber.e("Kernel Withdrawal($traceId): db insert fee snapshot")
                        tokenRepository.insertSafeSnapshot(UUID.nameUUIDFromBytes("$senderId:$feeTransactionHash".toByteArray()).toString(), senderId, receiverId, feeTransactionHash, feeTraceId, feeAssetId, feeAmount, "", SafeSnapshotType.snapshot)
                        Timber.e("Kernel Withdrawal($traceId): db raw transaction")
                        tokenRepository.insetRawTransaction(RawTransaction(withdrawalData.requestId, signWithdrawalResult.raw, formatDestination(destination, tag), RawTransactionType.WITHDRAWAL, OutputState.unspent, nowInUtc(), withdrawalUtxos.inscriptionHash))
                        Timber.e("Kernel Withdrawal($traceId): db insert fee raw transaction")
                        tokenRepository.insetRawTransaction(RawTransaction(feeData.requestId, signFeeResult.raw, receiverId, RawTransactionType.FEE, OutputState.unspent, nowInUtc(), null))
                    }
                    Timber.e("Kernel Withdrawal($traceId): db end")
                    jobManager.addJobInBackground(CheckBalanceJob(arrayListOf(assetIdToAsset(assetId), assetIdToAsset(feeAssetId))))
                } else {
                    Timber.e("Kernel Withdrawal($traceId): db begin")
                    appDatabase.runInTransaction {
                        if (signWithdrawalResult.change != null) {
                            val changeOutput = changeToOutput(signWithdrawalResult.change, asset, changeMask, data.last().keys, withdrawalUtxos.lastOutput)
                            Timber.e("Kernel Withdrawal($traceId): db insert change")
                            tokenRepository.insertOutput(changeOutput)
                        }
                        Timber.e("Kernel Withdrawal($traceId): db update Utxo ${withdrawalUtxos.ids}")
                        tokenRepository.updateUtxoToSigned(withdrawalUtxos.ids)
                        val transactionHash = signWithdrawal.hash
                        Timber.e("Kernel Withdrawal($traceId): db update insert snapshot")
                        tokenRepository.insertSafeSnapshot(
                            UUID.nameUUIDFromBytes("$senderId:$transactionHash".toByteArray()).toString(),
                            senderId,
                            "",
                            transactionHash,
                            traceId,
                            assetId,
                            amount,
                            memo,
                            SafeSnapshotType.withdrawal,
                            withdrawal =
                                SafeWithdrawal(
                                    "",
                                    destination,
                                ),
                        )
                        Timber.e("Kernel Withdrawal($traceId): db update raw transaction")
                        tokenRepository.insetRawTransaction(RawTransaction(withdrawalData.requestId, signWithdrawalResult.raw, formatDestination(destination, tag), RawTransactionType.WITHDRAWAL, OutputState.unspent, nowInUtc(), withdrawalUtxos.inscriptionHash))
                    }
                    Timber.e("Kernel Withdrawal($traceId): db end")
                    jobManager.addJobInBackground(CheckBalanceJob(arrayListOf(assetIdToAsset(assetId))))
                }
            }
            Timber.e("Kernel Withdrawal($traceId): withdrawal")
            val transactionRsp =
                executeWithRetry(5, {
                    tokenRepository.transactions(rawRequest)
                }, {
                    tokenRepository.getListTransactionsById(traceId)
                })
            if (transactionRsp.error != null) {
                Timber.e("Kernel Withdrawal($traceId): withdrawal error ${transactionRsp.errorDescription}")
                reportException(Throwable("Transaction Error ${transactionRsp.errorDescription}"))
                tokenRepository.updateRawTransaction(traceId, OutputState.signed.name)
                tokenRepository.updateRawTransaction(feeTraceId, OutputState.signed.name)
                return transactionRsp
            } else {
                tokenRepository.updateRawTransaction(traceId, OutputState.signed.name)
                tokenRepository.updateRawTransaction(feeTraceId, OutputState.signed.name)
            }
            jobManager.addJobInBackground(SyncOutputJob())
            Timber.e("Kernel Withdrawal($traceId): withdrawal end")
            return withdrawalRequestResponse
        }

        suspend fun kernelAddressTransaction(
            assetId: String,
            kernelAddress: String,
            amount: String,
            pin: String,
            trace: String,
            memo: String?,
            reference: String?,
        ): MixinResponse<*> {
            val asset = assetIdToAsset(assetId)
            val context = MixinApplication.appContext
            val tipPriv = tip.getOrRecoverTipPriv(context, pin).getOrThrow()
            val spendKey = tip.getSpendPrivFromEncryptedSalt(tip.getMnemonicFromEncryptedPreferences(context), tip.getEncryptedSalt(context), pin, tipPriv)
            val utxoWrapper = UtxoWrapper(packUtxo(asset, amount))

            Timber.e("Kernel Address Transaction($trace): begin")
            val rawTransaction = tokenRepository.findRawTransaction(trace)
            if (rawTransaction?.state == OutputState.unspent) {
                Timber.e("Kernel Address Transaction($trace): sync restore")
                return innerTransaction(rawTransaction.rawTransaction, trace, listOf(), utxoWrapper.inscriptionHash)
            }

            val senderId = Session.getAccountId()!!
            Timber.e("Kernel Address Transaction($trace): request ghost key")
            val ghostKeyResponse = tokenRepository.ghostKey(buildKernelTransferGhostKeyRequest(senderId, trace))
            if (ghostKeyResponse.error != null) {
                Timber.e("Kernel Address Transaction($trace): request ghost key ${ghostKeyResponse.errorDescription}")
                return ghostKeyResponse
            }
            val data = ghostKeyResponse.data!!

            val input = utxoWrapper.input

            val changeKeys = data.first().keys.joinToString(",")
            val changeMask = data.first().mask

            val tx = Kernel.buildTxToKernelAddress(asset, amount, kernelAddress, input, changeKeys, changeMask, memo, reference ?: "")
            Timber.e("Kernel Address Transaction($trace): request transaction ${utxoWrapper.ids.joinToString(", ")}")
            val transactionResponse = tokenRepository.transactionRequest(listOf(TransactionRequest(tx.raw, trace)))
            if (transactionResponse.error != null) {
                Timber.e("Kernel Address Transaction($trace): request transaction error ${transactionResponse.errorDescription}")
                return transactionResponse
            } else if ((transactionResponse.data?.size ?: 0) > 1) {
                Timber.e("Kernel Address Transaction($trace): Parameter exception")
                throw IllegalArgumentException("Parameter exception")
            } else if (transactionResponse.data?.first()?.state != OutputState.unspent.name) {
                Timber.e("Kernel Address Transaction($trace): Transfer is already paid")
                throw IllegalArgumentException("Transfer is already paid")
            }
            // Workaround with only the case of a single transfer
            val views = transactionResponse.data!!.first().views!!.joinToString(",")
            val keys = utxoWrapper.formatKeys
            val sign = Kernel.signTx(tx.raw, keys, views, spendKey.toHex(), false)
            val signResult = SignResult(sign.raw, sign.change)
            Timber.e("Kernel Address Transaction($trace): db begin")
            withContext(SINGLE_DB_THREAD) {
                appDatabase.runInTransaction {
                    if (signResult.change != null) {
                        val changeOutput = changeToOutput(signResult.change, asset, changeMask, data.last().keys, utxoWrapper.lastOutput)
                        Timber.e("Kernel Address Transaction($trace): sign db insert change")
                        tokenRepository.insertOutput(changeOutput)
                    }
                    val transactionHash = sign.hash
                    Timber.e("Kernel Address Transaction($trace): sign db insert snapshot")
                    tokenRepository.insertSafeSnapshot(UUID.nameUUIDFromBytes("$senderId:$transactionHash".toByteArray()).toString(), senderId, kernelAddress, transactionHash, trace, assetId, amount, memo, SafeSnapshotType.snapshot, reference = reference)
                    Timber.e("Kernel Address Transaction($trace): sign db insert raw transaction")
                    tokenRepository.insetRawTransaction(RawTransaction(transactionResponse.data!!.first().requestId, signResult.raw, "", RawTransactionType.TRANSFER, OutputState.unspent, nowInUtc(), utxoWrapper.inscriptionHash))
                    Timber.e("Kernel Address Transaction($trace): sign db mark utxo ${utxoWrapper.ids.joinToString(", ")}")
                    tokenRepository.updateUtxoToSigned(utxoWrapper.ids)
                    Timber.e("Kernel Address Transaction: sign end")
                }
            }
            Timber.e("Kernel Address Transaction($trace): db end")
            jobManager.addJobInBackground(CheckBalanceJob(arrayListOf(assetIdToAsset(assetId))))
            val innerTransactionResponse = innerTransaction(signResult.raw, trace, listOf(), utxoWrapper.inscriptionHash)
            Timber.e("Kernel Address Transaction($trace): end")
            return innerTransactionResponse
        }

        suspend fun kernelTransaction(
            assetId: String,
            receiverIds: List<String>,
            threshold: Byte,
            amount: String,
            pin: String,
            trace: String,
            memo: String?,
            reference: String? = null,
            inscriptionHash: String? = null,
            release: Boolean? = null,
        ): MixinResponse<*> {
            val context = MixinApplication.appContext
            val isConsolidation = receiverIds.size == 1 && receiverIds.first() == Session.getAccountId()
            val asset = assetIdToAsset(assetId)
            val tipPriv = tip.getOrRecoverTipPriv(context, pin).getOrThrow()
            val spendKey = tip.getSpendPrivFromEncryptedSalt(tip.getMnemonicFromEncryptedPreferences(context), tip.getEncryptedSalt(context), pin, tipPriv)
            val utxoWrapper = UtxoWrapper(packUtxo(asset, amount, inscriptionHash))
            Timber.e("Kernel Transaction($trace): begin")
            val rawTransaction = tokenRepository.findRawTransaction(trace)
            if (rawTransaction != null) {
                Timber.e("Kernel Transaction($trace): sync restore")
                return innerTransaction(rawTransaction.rawTransaction, trace, receiverIds, if (release == true) null else utxoWrapper.inscriptionHash)
            }
            Timber.e("Kernel Transaction($trace): request ghost key")
            val senderIds = listOf(Session.getAccountId()!!)
            val ghostKeyResponse = tokenRepository.ghostKey(buildGhostKeyRequest(receiverIds, senderIds, trace))
            if (ghostKeyResponse.error != null) {
                Timber.e("Kernel Transaction($trace): request ghost key ${ghostKeyResponse.errorDescription}")
                return ghostKeyResponse
            }
            val data = ghostKeyResponse.data!!

            val input = utxoWrapper.input
            val receiverKeys = data.first().keys.joinToString(",")
            val receiverMask = data.first().mask

            val changeKeys = data.last().keys.joinToString(",")
            val changeMask = data.last().mask
            val tx = Kernel.buildTx(asset, amount, threshold.toInt(), receiverKeys, receiverMask, input, changeKeys, changeMask, memo, reference ?: "")

            Timber.e("Kernel Transaction($trace): request transaction ${utxoWrapper.ids.joinToString(", ")}")
            val transactionResponse = tokenRepository.transactionRequest(listOf(TransactionRequest(tx.raw, trace)))
            if (transactionResponse.error != null) {
                Timber.e("Kernel Transaction($trace): request transaction error ${transactionResponse.errorDescription}")
                return transactionResponse
            } else if ((transactionResponse.data?.size ?: 0) > 1) {
                Timber.e("Kernel Transaction($trace): Parameter exception")
                throw IllegalArgumentException("Parameter exception")
            } else if (transactionResponse.data?.first()?.state != OutputState.unspent.name) {
                Timber.e("Kernel Transaction($trace): Transfer is already paid")
                throw IllegalArgumentException("Transfer is already paid")
            }
            // Workaround with only the case of a single transfer
            val views = transactionResponse.data!!.first().views!!.joinToString(",")
            val keys = utxoWrapper.formatKeys
            val sign = Kernel.signTx(tx.raw, keys, views, spendKey.toHex(), false)
            val signResult = SignResult(sign.raw, sign.change)
            Timber.e("Kernel Transaction($trace): db begin")
            withContext(SINGLE_DB_THREAD) {
                appDatabase.runInTransaction {
                    if (signResult.change != null) {
                        val changeOutput = changeToOutput(signResult.change, asset, changeMask, data.last().keys, utxoWrapper.lastOutput)
                        Timber.e("Kernel Transaction($trace): sign db insert change")
                        tokenRepository.insertOutput(changeOutput)
                    }
                    if (isConsolidation) {
                        val consolidationOutput = consolidationOutput(sign.hash, asset, amount, receiverMask, data.first().keys, utxoWrapper.lastOutput)
                        Timber.e("Kernel Transaction($trace): sign db insert consolidation")
                        tokenRepository.insertOutput(consolidationOutput)
                    }
                    if (!isConsolidation) {
                        val transactionHash = sign.hash
                        val opponentId =
                            if (receiverIds.size == 1) {
                                receiverIds.first()
                            } else {
                                ""
                            }
                        Timber.e("Kernel Transaction($trace): sign db insert snapshot")
                        tokenRepository.insertSafeSnapshot(UUID.nameUUIDFromBytes("${senderIds.first()}:$transactionHash".toByteArray()).toString(), senderIds.first(), opponentId, transactionHash, trace, assetId, amount, memo, SafeSnapshotType.snapshot, reference = reference ?: (if (release == true) null else inscriptionHash))
                    }
                    Timber.e("Kernel Transaction($trace): sign db insert raw transaction")
                    tokenRepository.insetRawTransaction(RawTransaction(transactionResponse.data!!.first().requestId, signResult.raw, receiverIds.joinToString(","), RawTransactionType.TRANSFER, OutputState.unspent, nowInUtc(), if (release == true) null else utxoWrapper.inscriptionHash))
                    Timber.e("Kernel Transaction($trace): sign db mark utxo ${utxoWrapper.ids.joinToString(", ")}")
                    tokenRepository.updateUtxoToSigned(utxoWrapper.ids)
                    Timber.e("Kernel Transaction: sign end")
                }
            }
            Timber.e("Kernel Transaction($trace): db end")
            jobManager.addJobInBackground(CheckBalanceJob(arrayListOf(assetIdToAsset(assetId))))
            val innerTransactionResponse = innerTransaction(signResult.raw, trace, receiverIds, if (release == true) null else utxoWrapper.inscriptionHash, isConsolidation)
            Timber.e("Kernel Transaction($trace): end")
            return innerTransactionResponse
        }

        private suspend fun innerTransaction(
            raw: String,
            traceId: String,
            receiverIds: List<String>,
            inscriptionHash: String?,
            isConsolidation: Boolean = false,
        ): MixinResponse<List<TransactionResponse>> {
            Timber.e("Kernel Transaction($traceId): innerTransaction")
            val transactionRsp =
                executeWithRetry(5, {
                    tokenRepository.transactions(listOf(TransactionRequest(raw, traceId)))
                }, {
                    tokenRepository.getListTransactionsById(traceId)
                })
            if (transactionRsp.error != null) {
                Timber.e("Kernel Transaction($traceId): innerTransaction error ${transactionRsp.errorDescription}")
                reportException(Throwable("Transaction Error ${transactionRsp.errorDescription}"))
                tokenRepository.updateRawTransaction(transactionRsp.data!!.first().requestId, OutputState.signed.name)
                return transactionRsp
            } else {
                Timber.e("Kernel Transaction($traceId): innerTransaction update raw transaction")
                tokenRepository.updateRawTransaction(transactionRsp.data!!.first().requestId, OutputState.signed.name)
            }
            if (receiverIds.size == 1 && !isConsolidation) {
                // Workaround with only the case of a single transfer
                val receiverId = receiverIds.first()
                val user = tokenRepository.findUser(receiverId)
                if (user != null && user.userId != Session.getAccountId() && !user.notMessengerUser()) {
                    val conversationId = generateConversationId(transactionRsp.data!!.first().userId, receiverId)
                    initConversation(conversationId, transactionRsp.data!!.first().userId, receiverId)
                    Timber.e("Kernel Transaction($traceId): innerTransaction insertSnapshotMessage $conversationId")
                    tokenRepository.insertSnapshotMessage(transactionRsp.data!!.first(), conversationId, inscriptionHash)
                }
            } else if (receiverIds.size > 1) {
                Timber.e("Kernel Transaction($traceId): innerTransaction insertSnapshotMessage")
                tokenRepository.insertSnapshotMessage(transactionRsp.data!!.first(), "", inscriptionHash)
            }
            jobManager.addJobInBackground(SyncOutputJob())
            Timber.e("Kernel Transaction($traceId): innerTransaction end")
            return transactionRsp
        }

        suspend fun invoiceTransaction(pin: String, invoice: MixinInvoice): MixinResponse<*> {
            val context = MixinApplication.appContext
            val tipPriv = tip.getOrRecoverTipPriv(context, pin).getOrThrow()
            tip.getSpendPrivFromEncryptedSalt(tip.getMnemonicFromEncryptedPreferences(context), tip.getEncryptedSalt(context), pin, tipPriv)
            val spendKey = tip.getSpendPrivFromEncryptedSalt(tip.getMnemonicFromEncryptedPreferences(context), tip.getEncryptedSalt(context), pin, tipPriv)
            val recipient = invoice.recipient
            val senderIds = listOf(Session.getAccountId()!!)
            val verifiedTransactions = mutableListOf<VerifiedTransactionData>()
            val signedTransactions = mutableListOf<SignedTransaction>()
            Timber.e("Kernel Invoice Transaction(${invoice.entries.joinToString(",") { it.traceId }}): begin")
            invoice.entries.forEach { entry ->
                val amount = entry.amountString()
                val assetId = entry.assetId
                val asset = assetIdToAsset(assetId)
                val trace = entry.traceId
                val data = if (recipient.uuidMembers.isNotEmpty()) {
                    val ghostKeyResponse = tokenRepository.ghostKey(buildGhostKeyRequest(recipient.uuidMembers, senderIds, trace))
                    if (ghostKeyResponse.error != null) {
                        Timber.e("Kernel Invoice Transaction($trace): request ghost key ${ghostKeyResponse.errorDescription}")
                        return ghostKeyResponse
                    }
                    ghostKeyResponse.data!!
                } else if (recipient.xinMembers.isNotEmpty()) {
                    val ghostKeyResponse = tokenRepository.ghostKey(buildKernelTransferGhostKeyRequest(senderIds.first(), trace))
                    if (ghostKeyResponse.error != null) {
                        Timber.e("Kernel Invoice Transaction($trace): request ghost key ${ghostKeyResponse.errorDescription}")
                        return ghostKeyResponse
                    }
                    ghostKeyResponse.data!!
                } else {
                    throw IllegalArgumentException("Transfer has no recipient")
                    null
                } ?: throw IllegalArgumentException("Transfer has no recipient")
                Timber.e("Kernel Invoice Transaction UtxoWrapper: $amount $assetId $asset")
                val utxoWrapper = UtxoWrapper(packUtxo(asset, amount, null, false))
                val input = utxoWrapper.input
                val receiverKeys = data.first().keys.joinToString(",")
                val receiverMask = data.first().mask

                val changeKeys = data.last().keys.joinToString(",")
                val changeMask = data.last().mask
                val reference = entry.references.joinToString(",") { reference ->
                    if (reference is Reference.IndexValue) {
                        verifiedTransactions.getOrNull(reference.value)?.hash ?: throw IllegalArgumentException("Reference not found")
                    } else if (reference is Reference.HashValue) {
                        reference.value
                    } else {
                        throw IllegalArgumentException("Reference type not supported")
                        ""
                    }
                }
                val tx = Kernel.buildTx(asset, amount, recipient.threshold.toInt(), receiverKeys, receiverMask, input, changeKeys, changeMask, String(entry.extra), reference)
                verifiedTransactions.add(VerifiedTransactionData(trace, tx.raw, tx.hash, utxoWrapper, asset, assetId, amount, changeMask, data.last().keys, entry.extra, reference))
            }
            val verifyTransaction = tokenRepository.transactionRequest(verifiedTransactions.map { TransactionRequest(it.raw, it.trace) })
            if (verifyTransaction.error != null) {
                Timber.e("Kernel Invoice Transaction: request transaction error ${verifyTransaction.errorDescription}")
                return verifyTransaction
            } else if ((verifyTransaction.data?.size ?: 0) != verifiedTransactions.size) {
                Timber.e("Kernel Invoice Transaction: Parameter exception")
                throw IllegalArgumentException("Parameter exception")
            } else if (verifyTransaction.data?.any { it.state != OutputState.unspent.name } == true) {
                Timber.e("Kernel Invoice Transaction: Transfer is already paid")
                throw IllegalArgumentException("Transfer is already paid")
            }

            verifyTransaction.data!!.forEachIndexed { index, data ->
                val verifyTransactionData = verifiedTransactions[index]
                val views = data.views!!.joinToString(",")
                val keys = verifyTransactionData.utxoWrapper.formatKeys
                val sign = Kernel.signTx(verifyTransactionData.raw, keys, views, spendKey.toHex(), false)
                val signResult = SignResult(sign.raw, sign.change)
                signedTransactions.add(SignedTransaction(verifyTransactionData.trace, signResult, verifyTransactionData.utxoWrapper.ids, verifyTransactionData.asset, verifyTransactionData.assetId, sign.hash, verifyTransactionData.changeMask, verifyTransactionData.keys, verifyTransactionData.utxoWrapper.lastOutput, verifyTransactionData.amount, String(verifyTransactionData.extra), verifyTransactionData.reference))
            }

            withContext(SINGLE_DB_THREAD) {
                appDatabase.runInTransaction {
                    signedTransactions.forEach { t ->
                        if (t.signResult.change != null) {
                            val changeOutput = changeToOutput(t.signResult.change, t.asset, t.changeMask, t.keys, t.lastOutput)
                            Timber.e("Kernel Invoice Transaction(${t.trace}): sign db insert change")
                            tokenRepository.insertOutput(changeOutput)
                        }
                        val transactionHash = t.transactionHash
                        val opponentId = invoice.recipient.uuidMembers.firstOrNull() ?: ""
                        Timber.e("Kernel Invoice Transaction(${t.trace}): sign db insert snapshot, memo${t.memo}")
                        tokenRepository.insertSafeSnapshot(UUID.nameUUIDFromBytes("${senderIds.first()}:$transactionHash".toByteArray()).toString(), senderIds.first(), opponentId, transactionHash, t.trace, t.assetId, t.amount, t.memo, SafeSnapshotType.snapshot, reference = t.reference)
                        Timber.e("Kernel Invoice Transaction(${t.trace}): sign db insert raw transaction")
                        tokenRepository.insetRawTransaction(
                            RawTransaction(
                                t.trace, t.signResult.raw,
                                if (invoice.recipient.uuidMembers.isEmpty()) {
                                    ""
                                } else {
                                    invoice.recipient.uuidMembers.joinToString(",")
                                }, RawTransactionType.TRANSFER, OutputState.unspent, nowInUtc(), null
                            )
                        )
                        Timber.e("Kernel Invoice Transaction(${t.trace}): sign db mark utxo ${t.utxoWrapperIds.joinToString(", ")}")
                        tokenRepository.updateUtxoToSigned(t.utxoWrapperIds)
                        Timber.e("Kernel Invoice Transaction(${t.trace}): sign db end")
                    }
                }
            }
            val signedResponse = tokenRepository.transactions(signedTransactions.map { TransactionRequest(it.signResult.raw, it.trace) })
            if (signedResponse.isSuccess) {
                withContext(SINGLE_DB_THREAD) {
                    appDatabase.runInTransaction {
                        Timber.e("Kernel Invoice Transaction: sign db begin")
                        signedResponse.data?.forEach {
                            tokenRepository.updateRawTransaction(it.requestId, OutputState.signed.name)
                        }
                        Timber.e("Kernel Invoice Transaction: sign db end")
                    }
                }

                if (invoice.recipient.uuidMembers.size == 1) {
                    val receiverId = invoice.recipient.uuidMembers.first()
                    val user = tokenRepository.findUser(receiverId)
                    if (user != null && user.userId != Session.getAccountId() && !user.notMessengerUser()) {
                        val conversationId = generateConversationId(signedResponse.data!!.first().userId, receiverId)
                        initConversation(conversationId, signedResponse.data!!.first().userId, receiverId)
                        signedResponse.data?.forEach { t ->
                            Timber.e("Kernel Invoice Transaction(${t.requestId}): innerTransaction insertSnapshotMessage $conversationId")
                            tokenRepository.insertSnapshotMessage(t, conversationId, null)
                        }
                    }
                }
            }

            Timber.e("Kernel Invoice Transaction: transaction end")
            return signedResponse
        }

        private fun initConversation(
            conversationId: String,
            senderId: String,
            recipientId: String,
        ) {
            val c = conversationRepo.getConversation(conversationId)
            if (c != null) return
            val createdAt = nowInUtc()
            val conversation =
                createConversation(
                    conversationId,
                    ConversationCategory.CONTACT.name,
                    recipientId,
                    ConversationStatus.START.ordinal,
                )
            val participants =
                arrayListOf(
                    Participant(conversationId, senderId, "", createdAt),
                    Participant(conversationId, recipientId, "", createdAt),
                )
            conversationRepo.syncInsertConversation(conversation, participants)
        }

        private suspend fun packUtxo(
            asset: String,
            amount: String,
            inscriptionHash: String? = null,
            ignoreZero: Boolean = true,
        ): List<Output> {
            val desiredAmount = BigDecimal(amount)
            val candidateOutputs = tokenRepository.findOutputs(maxUtxoCount, asset, inscriptionHash, ignoreZero)

            if (candidateOutputs.isEmpty()) {
                throw EmptyUtxoException
            }

            val selectedOutputs = mutableListOf<Output>()
            var totalSelectedAmount = BigDecimal.ZERO

            candidateOutputs.forEach { output ->
                val outputAmount = BigDecimal(output.amount)
                selectedOutputs.add(output)
                totalSelectedAmount += outputAmount
                if (totalSelectedAmount >= desiredAmount) {
                    return selectedOutputs
                }
            }

            if (selectedOutputs.size >= maxUtxoCount) {
                throw MaxCountNotEnoughUtxoException
            }

            if (totalSelectedAmount < desiredAmount) {
                throw NotEnoughUtxoException
            }

            throw Exception("Impossible")
        }

        suspend fun checkUtxoSufficiency(
            assetId: String,
            amount: String,
        ): String? {
            val desiredAmount = BigDecimal(amount)
            val candidateOutputs = tokenRepository.findOutputs(maxUtxoCount, assetIdToAsset(assetId))
            if (candidateOutputs.isEmpty()) {
                return null
            }

            val selectedOutputs = mutableListOf<Output>()
            var totalSelectedAmount = BigDecimal.ZERO
            var anyNotConfirmed = false
            candidateOutputs.forEach { output ->
                if (output.sequence == 0L) {
                    anyNotConfirmed = true
                }
                val outputAmount = BigDecimal(output.amount)
                selectedOutputs.add(output)
                totalSelectedAmount += outputAmount
                if (totalSelectedAmount >= desiredAmount) {
                    if (anyNotConfirmed) {
                        // Refresh when there is an undetermined UTXO
                        jobManager.addJobInBackground(SyncOutputJob())
                    }
                    return if (anyNotConfirmed) ""
                    else null
                }
            }

            if (selectedOutputs.size >= maxUtxoCount) {
                return totalSelectedAmount.toPlainString()
            }

            if (totalSelectedAmount < desiredAmount) {
                // Refresh when balance is insufficient
                jobManager.addJobInBackground(SyncOutputJob())
                if (anyNotConfirmed) return ""
            }

            throw Exception("Impossible")
        }

        suspend fun authorize(
            authorizationId: String,
            scopes: List<String>,
            pin: String?,
        ): MixinResponse<AuthorizationResponse> =
            accountRepository.authorize(authorizationId, scopes, pin)

        suspend fun paySuspend(request: TransferRequest) =
            withContext(Dispatchers.IO) {
                tokenRepository.paySuspend(request)
            }

        suspend fun getFees(
            id: String,
            destination: String,
        ) = tokenRepository.getFees(id, destination)

        suspend fun syncAddr(
            assetId: String,
            chainId: String,
            destination: String?,
            label: String?,
            tag: String?,
            code: String,
        ): MixinResponse<Address> =
            tokenRepository.syncAddr(
                AddressRequest(
                    assetId,
                    chainId,
                    destination,
                    tag,
                    label,
                    pinCipher.encryptPin(code, TipBody.forAddressAdd(assetId, destination, tag, label)),
                ),
            )

        suspend fun saveAddr(addr: Address) =
            withContext(Dispatchers.IO) {
                tokenRepository.saveAddr(addr)
            }

        suspend fun deleteAddr(
            id: String,
            code: String,
        ): MixinResponse<Unit> = tokenRepository.deleteAddr(id, pinCipher.encryptPin(code, TipBody.forAddressRemove(id)))

        suspend fun deleteLocalAddr(id: String) = tokenRepository.deleteLocalAddr(id)

        suspend fun simpleAssetItem(id: String) = tokenRepository.simpleAssetItem(id)

        fun findUserById(id: String): LiveData<User> = userRepository.findUserById(id)

        suspend fun suspendFindUserById(id: String) = userRepository.suspendFindUserById(id)

        fun updateRelationship(
            request: RelationshipRequest,
            report: Boolean = false,
        ) {
            jobManager.addJobInBackground(UpdateRelationshipJob(request, report))
        }

        suspend fun getParticipantsCount(conversationId: String) =
            conversationRepo.getParticipantsCount(conversationId)

        fun getConversationById(id: String) = conversationRepo.getConversationById(id)

        suspend fun getConversation(id: String) =
            withContext(Dispatchers.IO) {
                conversationRepo.getConversation(id)
            }

        fun findParticipantById(
            conversationId: String,
            userId: String,
        ) =
            conversationRepo.findParticipantById(conversationId, userId)

        suspend fun mute(
            duration: Long,
            conversationId: String? = null,
            senderId: String? = null,
            recipientId: String? = null,
        ): MixinResponse<ConversationResponse> {
            require(conversationId != null || (senderId != null && recipientId != null)) {
                "error data"
            }
            return if (conversationId != null) {
                val request = ConversationRequest(conversationId, ConversationCategory.GROUP.name, duration = duration)
                conversationRepo.muteSuspend(conversationId, request)
            } else {
                var cid = conversationRepo.getConversationIdIfExistsSync(recipientId!!)
                if (cid == null) {
                    cid = generateConversationId(senderId!!, recipientId)
                }
                val participantRequest = ParticipantRequest(recipientId, "")
                val request =
                    ConversationRequest(
                        cid,
                        ConversationCategory.CONTACT.name,
                        duration = duration,
                        participants = listOf(participantRequest),
                    )
                conversationRepo.muteSuspend(cid, request)
            }
        }

        suspend fun updateGroupMuteUntil(
            conversationId: String,
            muteUntil: String,
        ) {
            withContext(Dispatchers.IO) {
                conversationRepo.updateGroupMuteUntil(conversationId, muteUntil)
            }
        }

        suspend fun updateMuteUntil(
            id: String,
            muteUntil: String,
        ) {
            withContext(Dispatchers.IO) {
                userRepository.updateMuteUntil(id, muteUntil)
            }
        }

        suspend fun findAppById(id: String) = userRepository.findAppById(id)

        suspend fun getAppAndCheckUser(
            userId: String,
            updatedAt: String?,
        ) =
            userRepository.getAppAndCheckUser(userId, updatedAt)

        suspend fun findOrRefreshUsers(ids: List<String>) = userRepository.findOrRefreshUsers(ids)

        suspend fun refreshUser(id: String) = userRepository.refreshUser(id)

        suspend fun refreshSticker(id: String) = accountRepository.refreshSticker(id)

        suspend fun getAndSyncConversation(id: String) = conversationRepo.getAndSyncConversation(id)

        fun startGenerateAvatar(
            conversationId: String,
            list: List<String>? = null,
        ) {
            jobManager.addJobInBackground(GenerateAvatarJob(conversationId, list))
        }

        fun clearChat(conversationId: String) =
            viewModelScope.launch(Dispatchers.IO) {
                cleanMessageHelper.deleteMessageByConversationId(conversationId)
            }

        fun deleteConversation(conversationId: String) =
            viewModelScope.launch(Dispatchers.IO) {
                cleanMessageHelper.deleteMessageByConversationId(conversationId, true)
            }

        fun exitGroup(conversationId: String) {
            jobManager.addJobInBackground(
                ConversationJob(
                    conversationId = conversationId,
                    type = ConversationJob.TYPE_EXIT,
                ),
            )
        }

        fun updateGroup(
            conversationId: String,
            name: String? = null,
            iconBase64: String? = null,
            announcement: String? = null,
        ) {
            val request =
                ConversationRequest(
                    conversationId,
                    name = name,
                    iconBase64 = iconBase64,
                    announcement = announcement,
                )
            jobManager.addJobInBackground(
                ConversationJob(
                    conversationId = conversationId,
                    request = request,
                    type = ConversationJob.TYPE_UPDATE,
                ),
            )
        }

        fun refreshUser(
            userId: String,
            forceRefresh: Boolean,
        ) {
            jobManager.addJobInBackground(RefreshUserJob(listOf(userId), forceRefresh = forceRefresh))
        }

        fun refreshUsers(
            userIds: List<String>,
            conversationId: String?,
        ) {
            jobManager.addJobInBackground(
                RefreshUserJob(userIds, conversationId),
            )
        }

        suspend fun verifyPin(code: String): MixinResponse<Account> = accountRepository.verifyPin(code)

        suspend fun deactivate(
            pin: String,
            verificationId: String,
        ): MixinResponse<Account> = accountRepository.deactivate(pin, verificationId)

        fun trendingGifs(
            limit: Int,
            offset: Int,
        ): Observable<List<Gif>> =
            accountRepository.trendingGifs(limit, offset).map { it.data }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())

        fun searchGifs(
            query: String,
            limit: Int,
            offset: Int,
        ): Observable<List<Gif>> =
            accountRepository.searchGifs(query, limit, offset).map { it.data }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())

        suspend fun getLogoutTipBody(
            sessionId: String,
            pin: String,
        ): String = pinCipher.encryptPin(pin, TipBody.forLogout(sessionId))

        suspend fun deactivate(request: DeactivateRequest) = accountRepository.deactivate(request)

        suspend fun logout(sessionId: String, pin: String) =
            withContext(Dispatchers.IO) {
                val pinBase64 = getLogoutTipBody(sessionId, pin)
                accountRepository.logout(sessionId, pinBase64)
            }
        suspend fun findAssetItemById(assetId: String): TokenItem? =
            tokenRepository.findAssetItemById(assetId)

        suspend fun findAssetItemByCollectionHash(assetId: String): TokenItem? =
            tokenRepository.findAssetItemByCollectionHash(assetId)

        suspend fun findInscriptionCollectionByHash(hash: String): InscriptionCollection? =
            withContext(Dispatchers.IO) {
                tokenRepository.findInscriptionCollectionByHash(hash)
            }

        suspend fun findInscriptionByHash(hash: String): InscriptionItem? =
            withContext(Dispatchers.IO) {
                tokenRepository.findInscriptionByHash(hash)
            }

        suspend fun findUnspentOutputByHash(hash: String): Output? =
            withContext(Dispatchers.IO) {
                tokenRepository.findUnspentOutputByHash(hash)
            }

        suspend fun refreshAsset(assetId: String): TokenItem? {
            return withContext(Dispatchers.IO) {
                tokenRepository.findOrSyncAsset(assetId)
            }
        }

        suspend fun refreshAssetByInscription(collectionHash: String, instantiationHash: String): TokenItem? {
            return withContext(Dispatchers.IO) {
                tokenRepository.findOrSyncAssetByInscription(collectionHash, instantiationHash)
            }
        }

        suspend fun refreshInscriptionItem(assetId: String): InscriptionItem? {
            return withContext(Dispatchers.IO) {
                tokenRepository.getInscriptionItem(assetId)
            }
        }

        suspend fun refreshInscriptionCollection(hash: String): InscriptionCollection? {
            return withContext(Dispatchers.IO) {
                tokenRepository.getInscriptionCollection(hash)
            }
        }

        private suspend fun refreshSnapshot(snapshotId: String): SnapshotItem? {
            return withContext(Dispatchers.IO) {
                tokenRepository.refreshAndGetSnapshot(snapshotId)
            }
        }

        suspend fun preferences(request: AccountUpdateRequest) =
            withContext(Dispatchers.IO) {
                accountRepository.preferences(request)
            }

        suspend fun findMultiUsers(
            senders: Array<String>,
            receivers: Array<String>,
        ): Pair<ArrayList<User>, ArrayList<User>>? =
            withContext(Dispatchers.IO) {
                val userIds =
                    mutableSetOf<String>().apply {
                        addAll(senders)
                        addAll(receivers)
                    }
                val existUserIds = userRepository.findUserExist(userIds.toList())
                val queryUsers =
                    userIds.filter {
                        !existUserIds.contains(it)
                    }
                val users =
                    if (queryUsers.isNotEmpty()) {
                        handleMixinResponse(
                            invokeNetwork = {
                                userRepository.fetchUser(queryUsers)
                            },
                            successBlock = {
                                val userList = it.data
                                if (userList != null) {
                                    userRepository.upsertList(userList)
                                }
                                return@handleMixinResponse userRepository.findMultiUsersByIds(userIds)
                            },
                        ) ?: emptyList()
                    } else {
                        userRepository.findMultiUsersByIds(userIds)
                    }

                if (users.isEmpty()) return@withContext null
                val s = arrayListOf<User>()
                val r = arrayListOf<User>()
                users.forEach { u ->
                    if (u.userId in senders) {
                        s.add(u)
                    }
                    if (u.userId in receivers) {
                        r.add(u)
                    }
                }
                return@withContext Pair(s, r)
            }

        suspend fun signMultisigs(
            requestId: String,
            pin: String,
        ) =
            accountRepository.signMultisigs(
                requestId,
                PinRequest(
                    pinCipher.encryptPin(pin, TipBody.forMultisigRequestSign(requestId)),
                ),
            )

        suspend fun unlockMultisigs(
            requestId: String,
            pin: String,
        ) = accountRepository.unlockMultisigs(requestId, PinRequest(pinCipher.encryptPin(pin, TipBody.forMultisigRequestUnlock(requestId))))

        suspend fun cancelMultisigs(requestId: String) =
            withContext(Dispatchers.IO) {
                accountRepository.cancelMultisigs(requestId)
            }

        suspend fun getToken(tokenId: String) = accountRepository.getToken(tokenId)

        suspend fun signCollectibleTransfer(
            requestId: String,
            pinRequest: CollectibleRequest,
        ) = accountRepository.signCollectibleTransfer(requestId, pinRequest)

        suspend fun unlockCollectibleTransfer(
            requestId: String,
            pinRequest: CollectibleRequest,
        ) = accountRepository.unlockCollectibleTransfer(requestId, pinRequest)

        suspend fun cancelCollectibleTransfer(requestId: String) = accountRepository.cancelCollectibleTransfer(requestId)

        suspend fun transactions(
            rawTransactionsRequest: RawTransactionsRequest,
            pin: String,
        ): MixinResponse<Void> {
            rawTransactionsRequest.pin =
                pinCipher.encryptPin(
                    pin,
                    TipBody.forRawTransactionCreate(rawTransactionsRequest.assetId, "", rawTransactionsRequest.opponentMultisig.receivers.toList(), rawTransactionsRequest.opponentMultisig.threshold, rawTransactionsRequest.amount, rawTransactionsRequest.traceId, rawTransactionsRequest.memo),
                )
            return accountRepository.transactions(rawTransactionsRequest)
        }

        suspend fun findSnapshotById(snapshotId: String) = tokenRepository.findSnapshotById(snapshotId)

        fun insertSnapshot(snapshot: SafeSnapshot) =
            viewModelScope.launch(Dispatchers.IO) {
                tokenRepository.insertSnapshot(snapshot)
            }

        fun update(request: AccountUpdateRequest): Observable<MixinResponse<Account>> =
            accountRepository.update(request).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

        fun insertUser(user: User) =
            viewModelScope.launch(Dispatchers.IO) {
                userRepository.upsert(user)
            }

        suspend fun errorCount() = accountRepository.errorCount()

        fun refreshAccount() {
            jobManager.addJobInBackground(RefreshAccountJob())
        }

        fun observeSelf(): LiveData<User?> = userRepository.findSelf()

        fun observerFavoriteApps(userId: String) = accountRepository.observerFavoriteApps(userId)

        fun loadFavoriteApps(userId: String) {
            viewModelScope.launch(Dispatchers.IO) {
                handleMixinResponse(
                    invokeNetwork = { accountRepository.getUserFavoriteApps(userId) },
                    successBlock = {
                        it.data?.let { data ->
                            accountRepository.insertFavoriteApps(userId, data)
                            refreshAppNotExist(data.map { app -> app.appId })
                        }
                    },
                    exceptionBlock = {
                        return@handleMixinResponse true
                    },
                )
            }
        }

        private fun refreshAppNotExist(appIds: List<String>) =
            viewModelScope.launch(Dispatchers.IO) {
                accountRepository.refreshAppNotExist(appIds)
            }

        suspend fun createCircle(name: String) = userRepository.createCircle(name)

        suspend fun insertCircle(circle: Circle) {
            userRepository.insertCircle(circle)
        }

        suspend fun getIncludeCircleItem(conversationId: String): List<ConversationCircleManagerItem> = userRepository.getIncludeCircleItem(conversationId)

        suspend fun getOtherCircleItem(conversationId: String): List<ConversationCircleManagerItem> = userRepository.getOtherCircleItem(conversationId)

        suspend fun updateCircles(
            conversationId: String?,
            userId: String?,
            requests: List<ConversationCircleRequest>,
        ) =
            withContext(Dispatchers.IO) {
                conversationRepo.updateCircles(conversationId, userId, requests)
            }

        suspend fun deleteCircleConversation(
            conversationId: String,
            circleId: String,
        ) = userRepository.deleteCircleConversation(conversationId, circleId)

        suspend fun insertCircleConversation(circleConversation: CircleConversation) = userRepository.insertCircleConversation(circleConversation)

        suspend fun findCirclesNameByConversationId(conversationId: String) =
            userRepository.findCirclesNameByConversationId(conversationId)

        suspend fun getParticipantsWithoutBot(conversationId: String) =
            conversationRepo.getParticipantsWithoutBot(conversationId)

        suspend fun insertTrace(trace: Trace) = tokenRepository.insertTrace(trace)

        suspend fun suspendFindTraceById(traceId: String) = tokenRepository.suspendFindTraceById(traceId)

        suspend fun findLatestTrace(
            opponentId: String?,
            destination: String?,
            tag: String?,
            amount: String,
            assetId: String,
        ) =
            tokenRepository.findLatestTrace(opponentId, destination, tag, amount, assetId)

        suspend fun deletePreviousTraces() = tokenRepository.deletePreviousTraces()

        suspend fun suspendDeleteTraceById(traceId: String) = tokenRepository.suspendDeleteTraceById(traceId)

        suspend fun exportChat(
            conversationId: String,
            file: File,
        ) {
            var offset = 0
            val limit = 1000
            file.printWriter().use { writer ->
                while (true) {
                    val list = conversationRepo.getChatMessages(conversationId, offset, limit)
                    list.forEach { item ->
                        writer.println(item.toSimpleChat())
                    }
                    if (list.size < limit) {
                        break
                    } else {
                        offset += limit
                    }
                }
            }
        }

        suspend fun getAuthorizationByAppId(appId: String): AuthorizationResponse? =
            withContext(Dispatchers.IO) {
                return@withContext handleMixinResponse(
                    invokeNetwork = { accountRepository.getAuthorizationByAppId(appId) },
                    successBlock = {
                        return@handleMixinResponse it.data?.firstOrNull()
                    },
                )
            }

        suspend fun findSameConversations(
            selfId: String,
            userId: String,
        ) = conversationRepo.findSameConversations(selfId, userId)

        suspend fun fuzzySearchAssets(query: String?): List<TokenItem>? =
            if (query.isNullOrBlank()) {
                null
            } else {
                val escapedQuery = query.trim().escapeSql()
                tokenRepository.fuzzySearchAssetIgnoreAmount(escapedQuery)
            }

        suspend fun queryAsset(query: String): List<TokenItem> = tokenRepository.queryAsset(query)

        suspend fun findOrSyncAsset(assetId: String): TokenItem? {
            return withContext(Dispatchers.IO) {
                tokenRepository.findOrSyncAsset(assetId)
            }
        }

        suspend fun validateExternalAddress(
            assetId: String,
            destination: String,
            tag: String?,
        ) =
            accountRepository.validateExternalAddress(assetId, destination, tag)

        suspend fun findAssetIdByAssetKey(assetKey: String): String? =
            tokenRepository.findAssetIdByAssetKey(assetKey)

        suspend fun getAssetPrecisionById(assetId: String): MixinResponse<AssetPrecision> =
            tokenRepository.getAssetPrecisionById(assetId)

        suspend fun registerPublicKey(registerRequest: RegisterRequest) = utxoService.registerPublicKey(registerRequest)

        suspend fun getEncryptedTipBody(
            userId: String,
            pkHex: String,
            pin: String,
        ): String =
            pinCipher.encryptPin(pin, TipBody.forSequencerRegister(userId, pkHex))

        suspend fun getTransactionsById(traceId: String) = tokenRepository.getTransactionsById(traceId)

        suspend fun tokenEntry(ids: Array<String>) = tokenRepository.tokenEntry(ids)

        suspend fun tokenEntry() = tokenRepository.tokenEntry()

        suspend fun getMultisigs(requestId: String) = tokenRepository.getMultisigs(requestId)

        suspend fun transactionMultisigs(
            t: SafeMultisigsBiometricItem,
            pin: String,
        ): MixinResponse<TransactionResponse> {
            val context = MixinApplication.appContext
            val tipPriv = tip.getOrRecoverTipPriv(context, pin).getOrThrow()
            return if (t.action == "sign") {
                val spendKey = tip.getSpendPrivFromEncryptedSalt(tip.getMnemonicFromEncryptedPreferences(context), tip.getEncryptedSalt(context), pin, tipPriv)
                val sign = Kernel.signTransaction(t.raw, t.views, spendKey.toHex(), t.index.toLong(), false)
                tokenRepository.signTransactionMultisigs(t.traceId, TransactionRequest(sign.raw, t.traceId))
            } else if (t.action == "unlock") {
                tokenRepository.unlockTransactionMultisigs(t.traceId)
            } else {
                throw Exception("no support action" + t.action)
            }
        }

        suspend fun firstUnspentTransaction() =
            withContext(Dispatchers.IO) {
                tokenRepository.firstUnspentTransaction()
            }

        suspend fun getScheme(id: String) = accountRepository.getScheme(id)

        suspend fun findTokensExtra(asset: String) = tokenRepository.findTokensExtra(asset)

        fun sendTextMessage(
            conversationId: String,
            sender: User,
            content: String,
        ) {
            messenger.sendTextMessage(viewModelScope, conversationId, sender, content, EncryptCategory.PLAIN)
        }

        suspend fun refreshInscription(inscriptionHash: String): String? = withContext(Dispatchers.IO) {
            return@withContext tokenRepository.refreshInscription(inscriptionHash)
        }

        fun findAddressByReceiver(receiver: String, tag: String) = tokenRepository.findAddressByReceiver(receiver, tag)

        suspend fun checkMarketById(id: String): MarketItem? = withContext(Dispatchers.IO) {
            tokenRepository.checkMarketById(id)
        }

        fun syncAsset(assetId: String) {
            jobManager.addJobInBackground(RefreshAssetsJob(assetId))
        }

        suspend fun transactionsFetch(traceIds: List<String>) = tokenRepository.transactionsFetch(traceIds)
}
