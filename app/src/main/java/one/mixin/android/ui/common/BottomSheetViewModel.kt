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
import one.mixin.android.api.request.ParticipantRequest
import one.mixin.android.api.request.PinRequest
import one.mixin.android.api.request.RawTransactionsRequest
import one.mixin.android.api.request.RegisterRequest
import one.mixin.android.api.request.RelationshipRequest
import one.mixin.android.api.request.TransactionRequest
import one.mixin.android.api.request.TransferRequest
import one.mixin.android.api.request.buildGhostKeyRequest
import one.mixin.android.api.request.buildWithdrawalFeeGhostKeyRequest
import one.mixin.android.api.request.buildWithdrawalSubmitGhostKeyRequest
import one.mixin.android.api.response.AuthorizationResponse
import one.mixin.android.api.response.ConversationResponse
import one.mixin.android.api.response.TransactionResponse
import one.mixin.android.api.service.UtxoService
import one.mixin.android.crypto.PinCipher
import one.mixin.android.db.runInTransaction
import one.mixin.android.extension.base64RawURLEncode
import one.mixin.android.extension.escapeSql
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.toHex
import one.mixin.android.job.CheckBalanceJob
import one.mixin.android.job.ConversationJob
import one.mixin.android.job.GenerateAvatarJob
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshAccountJob
import one.mixin.android.job.RefreshConversationJob
import one.mixin.android.job.RefreshUserJob
import one.mixin.android.job.SyncOutputJob
import one.mixin.android.job.UpdateRelationshipJob
import one.mixin.android.repository.AccountRepository
import one.mixin.android.repository.TokenRepository
import one.mixin.android.repository.ConversationRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.session.Session
import one.mixin.android.tip.Tip
import one.mixin.android.tip.TipBody
import one.mixin.android.ui.common.biometric.EmptyUtxoException
import one.mixin.android.ui.common.biometric.MaxCountNotEnoughUtxoException
import one.mixin.android.ui.common.biometric.NotEnoughUtxoException
import one.mixin.android.ui.common.biometric.maxUtxoCount
import one.mixin.android.ui.common.message.CleanMessageHelper
import one.mixin.android.util.reportException
import one.mixin.android.vo.Account
import one.mixin.android.vo.Address
import one.mixin.android.vo.App
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.vo.AssetPrecision
import one.mixin.android.vo.Circle
import one.mixin.android.vo.CircleConversation
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.ConversationCircleManagerItem
import one.mixin.android.vo.ConversationStatus
import one.mixin.android.vo.Participant
import one.mixin.android.vo.safe.Output
import one.mixin.android.vo.safe.SafeSnapshot
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.Trace
import one.mixin.android.vo.User
import one.mixin.android.vo.assetIdToAsset
import one.mixin.android.vo.createConversation
import one.mixin.android.vo.generateConversationId
import one.mixin.android.vo.giphy.Gif
import one.mixin.android.vo.toSimpleChat
import one.mixin.android.vo.safe.RawTransaction
import one.mixin.android.vo.safe.UtxoWrapper
import one.mixin.android.vo.utxo.SignResult
import one.mixin.android.vo.utxo.changeToOutput
import timber.log.Timber
import java.io.File
import java.lang.IllegalArgumentException
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class BottomSheetViewModel @Inject internal constructor(
    private val accountRepository: AccountRepository,
    private val jobManager: MixinJobManager,
    private val userRepository: UserRepository,
    private val tokenRepository: TokenRepository,
    private val conversationRepo: ConversationRepository,
    private val cleanMessageHelper: CleanMessageHelper,
    private val pinCipher: PinCipher,
    private val tip: Tip,
    private val utxoService: UtxoService,
) : ViewModel() {
    suspend fun searchCode(code: String) = withContext(Dispatchers.IO) {
        accountRepository.searchCode(code)
    }

    fun join(code: String): Observable<MixinResponse<ConversationResponse>> =
        accountRepository.join(code).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

    fun refreshConversation(conversationId: String) {
        jobManager.addJobInBackground(RefreshConversationJob(conversationId))
    }

    suspend fun simpleAssetsWithBalance() = withContext(Dispatchers.IO) {
        tokenRepository.simpleAssetsWithBalance()
    }

    fun assetItems(): LiveData<List<TokenItem>> = tokenRepository.assetItems()

    fun assetItems(assetIds: List<String>): LiveData<List<TokenItem>> = tokenRepository.assetItems(assetIds)

    suspend fun findTokenItems(ids: List<String>): List<TokenItem> = tokenRepository.findTokenItems(ids)

    fun assetItemsWithBalance(): LiveData<List<TokenItem>> = tokenRepository.assetItemsWithBalance()

    suspend fun withdrawal(
        receiverId: String,
        traceId: String,
        assetId: String,
        feeAssetId: String,
        amount: String,
        feeAmount: String,
        destination: String,
        tag: String?,
        memo: String?,
        pin: String
    ): MixinResponse<*> {
        val isDifferentFee = feeAssetId != assetId
        val asset = assetIdToAsset(assetId)
        val feeAsset = assetIdToAsset(feeAssetId)
        val senderId = Session.getAccountId()!!
        val threshold = 1L

        val tipPriv = tip.getOrRecoverTipPriv(MixinApplication.appContext, pin).getOrThrow()
        val spendKey = tip.getSpendPrivFromEncryptedSalt(tip.getEncryptedSalt(MixinApplication.appContext), pin, tipPriv)

        val ghostKeyResponse = tokenRepository.ghostKey(
            if (isDifferentFee) {
                buildWithdrawalFeeGhostKeyRequest(receiverId, senderId, traceId)
            } else {
                buildWithdrawalSubmitGhostKeyRequest(receiverId, senderId, traceId)
            }
        )
        if (ghostKeyResponse.error != null) {
            return ghostKeyResponse
        }
        val data = ghostKeyResponse.data!!
        val withdrawalUtxos = UtxoWrapper(
            packUtxo(
                asset, if (isDifferentFee) amount else {
                    (BigDecimal(amount) + BigDecimal(feeAmount)).toPlainString()
                }
            )
        )
        val feeUtxos = if (isDifferentFee) {
            UtxoWrapper(packUtxo(feeAsset, feeAmount))
        } else {
            null
        }

        val feeKeys = data.first().keys.joinToString(",")
        val feeMask = data.first().mask

        val changeKeys = data.last().keys.joinToString(",")
        val changeMask = data.last().mask

        val withdrawalTx = Kernel.buildWithdrawalTx(asset, amount, destination, tag ?: "", if (isDifferentFee) "" else feeAmount, if (isDifferentFee) "" else feeKeys, if (isDifferentFee) "" else feeMask, withdrawalUtxos.input, changeKeys, changeMask, memo)
        val withdrawalRequests = mutableListOf(TransactionRequest(withdrawalTx.raw, traceId))

        val feeTx = if (isDifferentFee) {
            val feeChangeKeys = data[1].keys.joinToString(",")
            val feeChangeMask = data[1].mask
            val feeTx = Kernel.buildTx(feeAsset, feeAmount, threshold, feeKeys, feeMask, feeUtxos!!.input, feeChangeKeys, feeChangeMask, memo, withdrawalTx.hash)
            withdrawalRequests.add(TransactionRequest(feeTx, UUID.randomUUID().toString()))
            Timber.e("feeTx $feeTx")
            feeTx
        } else {
            null
        }

        val withdrawalRequestResponse = tokenRepository.transactionRequest(withdrawalRequests)
        if (withdrawalRequestResponse.error != null) {
            return withdrawalRequestResponse
        }
        val views = withdrawalRequestResponse.data!!.first().views.joinToString(",")
        val sign = Kernel.signTx(withdrawalTx.raw, withdrawalUtxos.formatKeys, views, spendKey.toHex())
        val signWithdrawalResult = SignResult(sign.raw, sign.change)
        val rawRequest = mutableListOf(TransactionRequest(signWithdrawalResult.raw, traceId))
        if (isDifferentFee) {
            val feeUtxos = UtxoWrapper(
                packUtxo(
                    feeAsset, feeAmount
                )
            )
            val feeViews = withdrawalRequestResponse.data!!.last().views.joinToString(",")
            val signFee = Kernel.signTx(feeTx, feeUtxos.formatKeys, feeViews, spendKey.toHex())
            val signFeeResult = SignResult(signFee.raw, signFee.change)
            rawRequest.add(TransactionRequest(signFeeResult.raw, UUID.randomUUID().toString()))
            runInTransaction {
                tokenRepository.updateUtxoToSigned(feeUtxos.ids)
                tokenRepository.updateUtxoToSigned(withdrawalUtxos.ids)
                if (signWithdrawalResult.change != null) {
                    val changeOutput = changeToOutput(signWithdrawalResult.change, asset, changeMask, data.last().keys, withdrawalUtxos.lastOutput)
                    tokenRepository.insertOutput(changeOutput)
                }
                if (signFeeResult.change != null) {
                    val changeOutput = changeToOutput(signFeeResult.change, asset, changeMask, data[1].keys, feeUtxos.lastOutput)
                    tokenRepository.insertOutput(changeOutput)
                }
                // todo save fee raw transaction
                tokenRepository.insetRawTransaction(RawTransaction(withdrawalRequestResponse.data!!.first().requestId, signWithdrawalResult.raw, receiverId, withdrawalRequestResponse.data!!.first().state,nowInUtc()))
            }
        } else {
            runInTransaction {
                if (signWithdrawalResult.change != null) {
                    val changeOutput = changeToOutput(signWithdrawalResult.change, asset, changeMask, data.last().keys, withdrawalUtxos.lastOutput)
                    tokenRepository.insertOutput(changeOutput)
                }
                tokenRepository.updateUtxoToSigned(withdrawalUtxos.ids)
                tokenRepository.insetRawTransaction(RawTransaction(withdrawalRequestResponse.data!!.first().requestId, signWithdrawalResult.raw, receiverId, withdrawalRequestResponse.data!!.first().state, nowInUtc()))
            }
        }
        val transactionRsp = tokenRepository.transactions(rawRequest)
        if (transactionRsp.error != null) {
            reportException(Throwable("Transaction Error ${transactionRsp.errorDescription}"))
            tokenRepository.deleteRawTransaction(traceId)
            return transactionRsp
        } else {
            tokenRepository.deleteRawTransaction(transactionRsp.data!!.first().requestId)
        }
        jobManager.addJobInBackground(SyncOutputJob())
        return transactionRsp
    }

    suspend fun newTransfer(
        assetId: String,
        receiverId: String,
        amount: String,
        pin: String,
        trace: String?,
        memo: String?,
    ): MixinResponse<*> {
        val asset = assetIdToAsset(assetId)
        val tipPriv = tip.getOrRecoverTipPriv(MixinApplication.appContext, pin).getOrThrow()
        val spendKey = tip.getSpendPrivFromEncryptedSalt(tip.getEncryptedSalt(MixinApplication.appContext), pin, tipPriv)
        val utxoWrapper = UtxoWrapper(packUtxo(asset, amount))
        if (trace != null) {
            val rawTransaction = tokenRepository.findRawTransaction(trace)
            if (rawTransaction != null) {
                return innerTransaction(rawTransaction.rawTransaction, trace, receiverId, assetId, amount, memo)
            }
        }

        val traceId = trace ?: UUID.randomUUID().toString()
        val senderId = Session.getAccountId()!!

        val threshold = 1L

        val ghostKeyResponse = tokenRepository.ghostKey(buildGhostKeyRequest(receiverId, senderId, traceId))
        if (ghostKeyResponse.error != null) {
            return ghostKeyResponse
        }
        val data = ghostKeyResponse.data!!

        val input = utxoWrapper.input
        val receiverKeys = data.first().keys.joinToString(",")
        val receiverMask = data.first().mask

        val changeKeys = data.last().keys.joinToString(",")
        val changeMask = data.last().mask

        val tx = Kernel.buildTx(asset, amount, threshold, receiverKeys, receiverMask, input, changeKeys, changeMask, memo, "")
        val transactionResponse = tokenRepository.transactionRequest(listOf(TransactionRequest(tx, traceId)))
        if (transactionResponse.error != null) {
            return transactionResponse
        }
        if ((transactionResponse.data?.size ?: 0) > 1) {
            throw IllegalArgumentException("Parameter exception")
        }
        // Workaround with only the case of a single transfer
        val views = transactionResponse.data!!.first().views.joinToString(",")
        val keys = utxoWrapper.formatKeys
        val sign = Kernel.signTx(tx, keys, views, spendKey.toHex())
        val signResult = SignResult(sign.raw, sign.change)
        runInTransaction {
            if (signResult.change != null) {
                val changeOutput = changeToOutput(signResult.change, asset, changeMask, data.last().keys, utxoWrapper.lastOutput)
                tokenRepository.insertOutput(changeOutput)
            }
            tokenRepository.insetRawTransaction(RawTransaction(transactionResponse.data!!.first().requestId, signResult.raw, receiverId, transactionResponse.data!!.first().state, nowInUtc()))
            tokenRepository.updateUtxoToSigned(utxoWrapper.ids)
        }
        jobManager.addJobInBackground(CheckBalanceJob(arrayListOf(assetIdToAsset(assetId))))
        return innerTransaction(signResult.raw, traceId, receiverId, assetId, amount, memo)
    }

    private suspend fun innerTransaction(raw: String, traceId: String, receiverId: String, assetId: String, amount: String, memo: String?): MixinResponse<List<TransactionResponse>> {
        val transactionRsp = tokenRepository.transactions(listOf(TransactionRequest(raw, traceId)))
        if (transactionRsp.error != null) {
            reportException(Throwable("Transaction Error ${transactionRsp.errorDescription}"))
            tokenRepository.deleteRawTransaction(traceId)
            return transactionRsp
        } else {
            tokenRepository.deleteRawTransaction(transactionRsp.data!!.first().requestId)
        }
        // Workaround with only the case of a single transfer
        val conversationId = generateConversationId(transactionRsp.data!!.first().userId, receiverId)
        initConversation(conversationId, transactionRsp.data!!.first().userId, receiverId)
        tokenRepository.insertSnapshotMessage(transactionRsp.data!!.first(), conversationId, assetId, amount, receiverId, memo)
        jobManager.addJobInBackground(SyncOutputJob())
        return transactionRsp
    }

    private fun initConversation(conversationId: String, senderId: String, recipientId: String) {
        val c = conversationRepo.getConversation(conversationId)
        if (c != null) return
        val createdAt = nowInUtc()
        val conversation = createConversation(
            conversationId,
            ConversationCategory.CONTACT.name,
            recipientId,
            ConversationStatus.START.ordinal,
        )
        val participants = arrayListOf(
            Participant(conversationId, senderId, "", createdAt),
            Participant(conversationId, recipientId, "", createdAt),
        )
        conversationRepo.syncInsertConversation(conversation, participants)
    }

    private suspend fun packUtxo(asset: String, amount: String): List<Output> {
        val desiredAmount = BigDecimal(amount)
        val candidateOutputs = tokenRepository.findOutputs(maxUtxoCount, asset)

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

    suspend fun authorize(authorizationId: String, scopes: List<String>, pin: String?): MixinResponse<AuthorizationResponse> =
        accountRepository.authorize(authorizationId, scopes, pin)

    suspend fun paySuspend(request: TransferRequest) = withContext(Dispatchers.IO) {
        tokenRepository.paySuspend(request)
    }

    suspend fun getFees(id: String, destination: String) = tokenRepository.getFees(id, destination)

    suspend fun syncAddr(
        assetId: String,
        destination: String?,
        label: String?,
        tag: String?,
        code: String,
    ): MixinResponse<Address> =
        tokenRepository.syncAddr(
            AddressRequest(
                assetId,
                destination,
                tag,
                label,
                pinCipher.encryptPin(code, TipBody.forAddressAdd(assetId, destination, tag, label)),
            ),
        )

    suspend fun saveAddr(addr: Address) = withContext(Dispatchers.IO) {
        tokenRepository.saveAddr(addr)
    }

    suspend fun deleteAddr(id: String, code: String): MixinResponse<Unit> = tokenRepository.deleteAddr(id, pinCipher.encryptPin(code, TipBody.forAddressRemove(id)))

    suspend fun deleteLocalAddr(id: String) = tokenRepository.deleteLocalAddr(id)

    suspend fun simpleAssetItem(id: String) = tokenRepository.simpleAssetItem(id)

    fun findUserById(id: String): LiveData<User> = userRepository.findUserById(id)

    suspend fun suspendFindUserById(id: String) = userRepository.suspendFindUserById(id)

    fun updateRelationship(request: RelationshipRequest, report: Boolean = false) {
        jobManager.addJobInBackground(UpdateRelationshipJob(request, report))
    }

    suspend fun getParticipantsCount(conversationId: String) =
        conversationRepo.getParticipantsCount(conversationId)

    fun getConversationById(id: String) = conversationRepo.getConversationById(id)

    suspend fun getConversation(id: String) = withContext(Dispatchers.IO) {
        conversationRepo.getConversation(id)
    }

    fun findParticipantById(conversationId: String, userId: String) =
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
            val request = ConversationRequest(
                cid,
                ConversationCategory.CONTACT.name,
                duration = duration,
                participants = listOf(participantRequest),
            )
            conversationRepo.muteSuspend(cid, request)
        }
    }

    suspend fun updateGroupMuteUntil(conversationId: String, muteUntil: String) {
        withContext(Dispatchers.IO) {
            conversationRepo.updateGroupMuteUntil(conversationId, muteUntil)
        }
    }

    suspend fun updateMuteUntil(id: String, muteUntil: String) {
        withContext(Dispatchers.IO) {
            userRepository.updateMuteUntil(id, muteUntil)
        }
    }

    suspend fun findAppById(id: String) = userRepository.findAppById(id)

    suspend fun getAppAndCheckUser(userId: String, updatedAt: String?) =
        userRepository.getAppAndCheckUser(userId, updatedAt)

    suspend fun refreshUser(id: String) = userRepository.refreshUser(id)

    suspend fun refreshSticker(id: String) = accountRepository.refreshSticker(id)

    suspend fun getAndSyncConversation(id: String) = conversationRepo.getAndSyncConversation(id)

    fun startGenerateAvatar(conversationId: String, list: List<String>? = null) {
        jobManager.addJobInBackground(GenerateAvatarJob(conversationId, list))
    }

    fun clearChat(conversationId: String) = viewModelScope.launch(Dispatchers.IO) {
        cleanMessageHelper.deleteMessageByConversationId(conversationId)
    }

    fun deleteConversation(conversationId: String) = viewModelScope.launch(Dispatchers.IO) {
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
        val request = ConversationRequest(
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

    fun refreshUser(userId: String, forceRefresh: Boolean) {
        jobManager.addJobInBackground(RefreshUserJob(listOf(userId), forceRefresh = forceRefresh))
    }

    fun refreshUsers(userIds: List<String>, conversationId: String?) {
        jobManager.addJobInBackground(
            RefreshUserJob(userIds, conversationId),
        )
    }

    suspend fun verifyPin(code: String): MixinResponse<Account> = accountRepository.verifyPin(code)

    suspend fun deactivate(pin: String, verificationId: String): MixinResponse<Account> = accountRepository.deactivate(pin, verificationId)

    fun trendingGifs(limit: Int, offset: Int): Observable<List<Gif>> =
        accountRepository.trendingGifs(limit, offset).map { it.data }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())

    fun searchGifs(query: String, limit: Int, offset: Int): Observable<List<Gif>> =
        accountRepository.searchGifs(query, limit, offset).map { it.data }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())

    suspend fun logout(sessionId: String) = withContext(Dispatchers.IO) {
        accountRepository.logout(sessionId)
    }

    suspend fun findAddressById(addressId: String, assetId: String): Pair<Address?, Boolean> = withContext(Dispatchers.IO) {
        val address = tokenRepository.findAddressById(addressId, assetId)
            ?: return@withContext tokenRepository.refreshAndGetAddress(addressId, assetId)
        return@withContext Pair(address, false)
    }

    suspend fun refreshAndGetAddress(addressId: String, assetId: String): Pair<Address?, Boolean> = withContext(Dispatchers.IO) {
        return@withContext tokenRepository.refreshAndGetAddress(addressId, assetId)
    }

    suspend fun findAssetItemById(assetId: String): TokenItem? =
        tokenRepository.findAssetItemById(assetId)

    suspend fun refreshAsset(assetId: String): TokenItem? {
        return withContext(Dispatchers.IO) {
            tokenRepository.findOrSyncAsset(assetId)
        }
    }

    private suspend fun refreshSnapshot(snapshotId: String): SnapshotItem? {
        return withContext(Dispatchers.IO) {
            tokenRepository.refreshAndGetSnapshot(snapshotId)
        }
    }

    suspend fun getSnapshotByTraceId(traceId: String): Pair<SnapshotItem, TokenItem>? {
        return withContext(Dispatchers.IO) {
            val localItem = tokenRepository.findSnapshotByTraceId(traceId)
            if (localItem != null) {
                var assetItem = findAssetItemById(localItem.assetId)
                if (assetItem != null) {
                    return@withContext Pair(localItem, assetItem)
                } else {
                    assetItem = refreshAsset(localItem.assetId)
                    if (assetItem != null) {
                        return@withContext Pair(localItem, assetItem)
                    } else {
                        return@withContext null
                    }
                }
            } else {
                handleMixinResponse(
                    invokeNetwork = {
                        tokenRepository.getTrace(traceId)
                    },
                    successBlock = { response ->
                        response.data?.let { snapshot ->
                            tokenRepository.insertSnapshot(snapshot)
                            val assetItem =
                                refreshAsset(snapshot.assetId) ?: return@handleMixinResponse null
                            val snapshotItem = tokenRepository.findSnapshotById(snapshot.snapshotId)
                                ?: return@handleMixinResponse null
                            return@handleMixinResponse Pair(snapshotItem, assetItem)
                        }
                    },
                )
            }
        }
    }

    suspend fun getSnapshotAndAsset(snapshotId: String): Pair<SnapshotItem, TokenItem>? {
        return withContext(Dispatchers.IO) {
            var snapshotItem = findSnapshotById(snapshotId)
            if (snapshotItem != null) {
                var assetItem = findAssetItemById(snapshotItem.assetId)
                if (assetItem != null) {
                    return@withContext Pair(snapshotItem, assetItem)
                } else {
                    assetItem = refreshAsset(snapshotItem.assetId)
                    if (assetItem != null) {
                        return@withContext Pair(snapshotItem, assetItem)
                    } else {
                        return@withContext null
                    }
                }
            } else {
                snapshotItem = refreshSnapshot(snapshotId)
                if (snapshotItem == null) {
                    return@withContext null
                } else {
                    var assetItem = findAssetItemById(snapshotItem.assetId)
                    if (assetItem != null) {
                        return@withContext Pair(snapshotItem, assetItem)
                    } else {
                        assetItem = refreshAsset(snapshotItem.assetId)
                        if (assetItem != null) {
                            return@withContext Pair(snapshotItem, assetItem)
                        } else {
                            return@withContext null
                        }
                    }
                }
            }
        }
    }

    suspend fun preferences(request: AccountUpdateRequest) = withContext(Dispatchers.IO) {
        accountRepository.preferences(request)
    }

    suspend fun searchAppByHost(query: String): List<App> {
        val escapedQuery = query.trim().escapeSql()
        return userRepository.searchAppByHost(escapedQuery)
    }

    suspend fun findMultiUsers(
        senders: Array<String>,
        receivers: Array<String>,
    ): Pair<ArrayList<User>, ArrayList<User>>? = withContext(Dispatchers.IO) {
        val userIds = mutableSetOf<String>().apply {
            addAll(senders)
            addAll(receivers)
        }
        val existUserIds = userRepository.findUserExist(userIds.toList())
        val queryUsers = userIds.filter {
            !existUserIds.contains(it)
        }
        val users = if (queryUsers.isNotEmpty()) {
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

    suspend fun signMultisigs(requestId: String, pin: String) =
        accountRepository.signMultisigs(
            requestId,
            PinRequest(
                pinCipher.encryptPin(pin, TipBody.forMultisigRequestSign(requestId)),
            ),
        )

    suspend fun unlockMultisigs(requestId: String, pin: String) = accountRepository.unlockMultisigs(requestId, PinRequest(pinCipher.encryptPin(pin, TipBody.forMultisigRequestUnlock(requestId))))

    suspend fun cancelMultisigs(requestId: String) = withContext(Dispatchers.IO) {
        accountRepository.cancelMultisigs(requestId)
    }

    suspend fun getToken(tokenId: String) = accountRepository.getToken(tokenId)

    suspend fun signCollectibleTransfer(requestId: String, pinRequest: CollectibleRequest) = accountRepository.signCollectibleTransfer(requestId, pinRequest)

    suspend fun unlockCollectibleTransfer(requestId: String, pinRequest: CollectibleRequest) = accountRepository.unlockCollectibleTransfer(requestId, pinRequest)

    suspend fun cancelCollectibleTransfer(requestId: String) = accountRepository.cancelCollectibleTransfer(requestId)

    suspend fun transactions(
        rawTransactionsRequest: RawTransactionsRequest,
        pin: String,
    ): MixinResponse<Void> {
        rawTransactionsRequest.pin = pinCipher.encryptPin(
            pin,
            TipBody.forRawTransactionCreate(rawTransactionsRequest.assetId, "", rawTransactionsRequest.opponentMultisig.receivers.toList(), rawTransactionsRequest.opponentMultisig.threshold, rawTransactionsRequest.amount, rawTransactionsRequest.traceId, rawTransactionsRequest.memo)
        )
        return accountRepository.transactions(rawTransactionsRequest)
    }

    suspend fun findSnapshotById(snapshotId: String) = tokenRepository.findSnapshotById(snapshotId)

    fun insertSnapshot(snapshot: SafeSnapshot) = viewModelScope.launch(Dispatchers.IO) {
        tokenRepository.insertSnapshot(snapshot)
    }

    fun update(request: AccountUpdateRequest): Observable<MixinResponse<Account>> =
        accountRepository.update(request).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

    fun insertUser(user: User) = viewModelScope.launch(Dispatchers.IO) {
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

    private suspend fun refreshAppNotExist(appIds: List<String>) = viewModelScope.launch(Dispatchers.IO) {
        accountRepository.refreshAppNotExist(appIds)
    }

    suspend fun createCircle(name: String) = userRepository.createCircle(name)

    suspend fun insertCircle(circle: Circle) {
        userRepository.insertCircle(circle)
    }

    suspend fun getIncludeCircleItem(conversationId: String): List<ConversationCircleManagerItem> = userRepository.getIncludeCircleItem(conversationId)

    suspend fun getOtherCircleItem(conversationId: String): List<ConversationCircleManagerItem> = userRepository.getOtherCircleItem(conversationId)

    suspend fun updateCircles(conversationId: String?, userId: String?, requests: List<ConversationCircleRequest>) = withContext(Dispatchers.IO) {
        conversationRepo.updateCircles(conversationId, userId, requests)
    }

    suspend fun deleteCircleConversation(conversationId: String, circleId: String) = userRepository.deleteCircleConversation(conversationId, circleId)

    suspend fun insertCircleConversation(circleConversation: CircleConversation) = userRepository.insertCircleConversation(circleConversation)

    suspend fun findCirclesNameByConversationId(conversationId: String) =
        userRepository.findCirclesNameByConversationId(conversationId)

    suspend fun findMultiUsersByIds(userIds: Set<String>) = userRepository.findMultiUsersByIds(userIds)

    suspend fun getParticipantsWithoutBot(conversationId: String) =
        conversationRepo.getParticipantsWithoutBot(conversationId)

    suspend fun insertTrace(trace: Trace) = tokenRepository.insertTrace(trace)

    suspend fun suspendFindTraceById(traceId: String) = tokenRepository.suspendFindTraceById(traceId)

    suspend fun findLatestTrace(opponentId: String?, destination: String?, tag: String?, amount: String, assetId: String) =
        tokenRepository.findLatestTrace(opponentId, destination, tag, amount, assetId)

    suspend fun deletePreviousTraces() = tokenRepository.deletePreviousTraces()

    suspend fun suspendDeleteTraceById(traceId: String) = tokenRepository.suspendDeleteTraceById(traceId)

    suspend fun exportChat(conversationId: String, file: File) {
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

    suspend fun getAuthorizationByAppId(appId: String): AuthorizationResponse? = withContext(Dispatchers.IO) {
        return@withContext handleMixinResponse(
            invokeNetwork = { accountRepository.getAuthorizationByAppId(appId) },
            successBlock = {
                return@handleMixinResponse it.data?.firstOrNull()
            },
        )
    }

    suspend fun findSameConversations(selfId: String, userId: String) = conversationRepo.findSameConversations(selfId, userId)

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

    suspend fun getExternalAddressFee(assetId: String, destination: String, tag: String?) =
        accountRepository.getExternalAddressFee(assetId, destination, tag)

    suspend fun findAssetIdByAssetKey(assetKey: String): String? =
        tokenRepository.findAssetIdByAssetKey(assetKey)

    suspend fun getAssetPrecisionById(assetId: String): MixinResponse<AssetPrecision> =
        tokenRepository.getAssetPrecisionById(assetId)

    suspend fun registerPublicKey(registerRequest: RegisterRequest) = utxoService.registerPublicKey(registerRequest)

    suspend fun getEncryptedTipBody(userId: String, pkHex: String, pin: String): String =
        pinCipher.encryptPin(pin, TipBody.forSequencerRegister(userId, pkHex))
}
