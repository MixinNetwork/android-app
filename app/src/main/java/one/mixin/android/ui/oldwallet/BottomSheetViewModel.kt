package one.mixin.android.ui.oldwallet

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.AccountUpdateRequest
import one.mixin.android.api.request.AddressRequest
import one.mixin.android.api.request.CollectibleRequest
import one.mixin.android.api.request.ConversationRequest
import one.mixin.android.api.request.DeactivateRequest
import one.mixin.android.api.request.ParticipantRequest
import one.mixin.android.api.request.PinRequest
import one.mixin.android.api.request.RawTransactionsRequest
import one.mixin.android.api.request.RelationshipRequest
import one.mixin.android.api.request.TransferRequest
import one.mixin.android.api.response.ConversationResponse
import one.mixin.android.crypto.PinCipher
import one.mixin.android.job.GenerateAvatarJob
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshConversationJob
import one.mixin.android.job.RefreshUserJob
import one.mixin.android.job.UpdateRelationshipJob
import one.mixin.android.repository.AccountRepository
import one.mixin.android.repository.ConversationRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.tip.TipBody
import one.mixin.android.vo.Account
import one.mixin.android.vo.Address
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.AssetPrecision
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.Snapshot
import one.mixin.android.vo.Trace
import one.mixin.android.vo.User
import one.mixin.android.vo.generateConversationId
import javax.inject.Inject

@HiltViewModel
class BottomSheetViewModel
    @Inject
    internal constructor(
        private val accountRepository: AccountRepository,
        private val jobManager: MixinJobManager,
        private val userRepository: UserRepository,
        private val assetRepository: AssetRepository,
        private val conversationRepo: ConversationRepository,
        private val pinCipher: PinCipher,
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

        suspend fun paySuspend(request: TransferRequest) =
            withContext(Dispatchers.IO) {
                assetRepository.paySuspend(request)
            }

        suspend fun transfer(
            assetId: String,
            userId: String,
            amount: String,
            code: String,
            trace: String?,
            memo: String?,
        ) = assetRepository.transfer(
            TransferRequest(
                assetId,
                userId,
                amount,
                pinCipher.encryptPin(code, TipBody.forTransfer(assetId, userId, amount, trace, memo)),
                trace,
                memo,
            ),
        )

        suspend fun syncAddr(
            assetId: String,
            destination: String?,
            label: String?,
            tag: String?,
            code: String,
        ): MixinResponse<Address> =
            assetRepository.syncAddr(
                AddressRequest(
                    assetId,
                    destination,
                    tag,
                    label,
                    pinCipher.encryptPin(code, TipBody.forAddressAdd(assetId, destination, tag, label)),
                ),
            )

        suspend fun saveAddr(addr: Address) =
            withContext(Dispatchers.IO) {
                assetRepository.saveAddr(addr)
            }

        suspend fun deleteAddr(
            id: String,
            code: String,
        ): MixinResponse<Unit> = assetRepository.deleteAddr(id, pinCipher.encryptPin(code, TipBody.forAddressRemove(id)))

        suspend fun deleteLocalAddr(id: String) = assetRepository.deleteLocalAddr(id)

        suspend fun simpleAssetItem(id: String) = assetRepository.simpleAssetItem(id)

        fun findUserById(id: String): LiveData<User> = userRepository.findUserById(id)

        suspend fun suspendFindUserById(id: String) = userRepository.suspendFindUserById(id)

        fun updateRelationship(
            request: RelationshipRequest,
            report: Boolean = false,
        ) {
            jobManager.addJobInBackground(UpdateRelationshipJob(request, report))
        }

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

        suspend fun refreshUser(id: String) = userRepository.refreshUser(id)

        suspend fun getAndSyncConversation(id: String) = conversationRepo.getAndSyncConversation(id)

        fun startGenerateAvatar(
            conversationId: String,
            list: List<String>? = null,
        ) {
            jobManager.addJobInBackground(GenerateAvatarJob(conversationId, list))
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

        suspend fun getDeactivateTipBody(
            userId: String,
            pin: String,
        ): String = pinCipher.encryptPin(pin, TipBody.forDeactivate(userId))

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

        suspend fun findAddressById(
            addressId: String,
            assetId: String,
        ): Pair<Address?, Boolean> =
            withContext(Dispatchers.IO) {
                val address =
                    assetRepository.findAddressById(addressId, assetId)
                        ?: return@withContext assetRepository.refreshAndGetAddress(addressId, assetId)
                return@withContext Pair(address, false)
            }

        suspend fun refreshAndGetAddress(
            addressId: String,
            assetId: String,
        ): Pair<Address?, Boolean> =
            withContext(Dispatchers.IO) {
                return@withContext assetRepository.refreshAndGetAddress(addressId, assetId)
            }

        suspend fun findAssetItemById(assetId: String): AssetItem? =
            assetRepository.findAssetItemById(assetId)

        suspend fun refreshAsset(assetId: String): AssetItem? {
            return withContext(Dispatchers.IO) {
                assetRepository.findOrSyncAsset(assetId)
            }
        }

        private suspend fun refreshSnapshot(snapshotId: String): SnapshotItem? {
            return withContext(Dispatchers.IO) {
                assetRepository.refreshAndGetSnapshot(snapshotId)
            }
        }

        suspend fun getSnapshotByTraceId(traceId: String): Pair<SnapshotItem, AssetItem>? {
            return withContext(Dispatchers.IO) {
                val localItem = assetRepository.findSnapshotByTraceId(traceId)
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
                            assetRepository.getTrace(traceId)
                        },
                        successBlock = { response ->
                            response.data?.let { snapshot ->
                                assetRepository.insertSnapshot(snapshot)
                                val assetItem =
                                    refreshAsset(snapshot.assetId) ?: return@handleMixinResponse null
                                val snapshotItem =
                                    assetRepository.findSnapshotById(snapshot.snapshotId)
                                        ?: return@handleMixinResponse null
                                return@handleMixinResponse Pair(snapshotItem, assetItem)
                            }
                        },
                    )
                }
            }
        }

        suspend fun getSnapshotAndAsset(snapshotId: String): Pair<SnapshotItem, AssetItem>? {
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
            rawTransactionsRequest.pin = pinCipher.encryptPin(pin, TipBody.forRawTransactionCreate(rawTransactionsRequest.assetId, "", rawTransactionsRequest.opponentMultisig.receivers.toList(), rawTransactionsRequest.opponentMultisig.threshold, rawTransactionsRequest.amount, rawTransactionsRequest.traceId, rawTransactionsRequest.memo))
            return accountRepository.transactions(rawTransactionsRequest)
        }

        suspend fun findSnapshotById(snapshotId: String) = assetRepository.findSnapshotById(snapshotId)

        fun insertSnapshot(snapshot: Snapshot) =
            viewModelScope.launch(Dispatchers.IO) {
                assetRepository.insertSnapshot(snapshot)
            }

        fun update(request: AccountUpdateRequest): Observable<MixinResponse<Account>> =
            accountRepository.update(request).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

        fun insertUser(user: User) =
            viewModelScope.launch(Dispatchers.IO) {
                userRepository.upsert(user)
            }

        suspend fun errorCount() = accountRepository.errorCount()

        suspend fun findMultiUsersByIds(userIds: Set<String>) = userRepository.findMultiUsersByIds(userIds)

        suspend fun insertTrace(trace: Trace) = assetRepository.insertTrace(trace)

        suspend fun suspendFindTraceById(traceId: String) = assetRepository.suspendFindTraceById(traceId)

        suspend fun findLatestTrace(
            opponentId: String?,
            destination: String?,
            tag: String?,
            amount: String,
            assetId: String,
        ) =
            assetRepository.findLatestTrace(opponentId, destination, tag, amount, assetId)

        suspend fun deletePreviousTraces() = assetRepository.deletePreviousTraces()

        suspend fun suspendDeleteTraceById(traceId: String) = assetRepository.suspendDeleteTraceById(traceId)

        suspend fun findOrSyncAsset(assetId: String): AssetItem? {
            return withContext(Dispatchers.IO) {
                assetRepository.findOrSyncAsset(assetId)
            }
        }

        suspend fun findAssetIdByAssetKey(assetKey: String): String? =
            assetRepository.findAssetIdByAssetKey(assetKey)

        suspend fun getAssetPrecisionById(assetId: String): MixinResponse<AssetPrecision> =
            assetRepository.getAssetPrecisionById(assetId)
    }
