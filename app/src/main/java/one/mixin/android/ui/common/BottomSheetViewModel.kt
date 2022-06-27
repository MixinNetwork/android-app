package one.mixin.android.ui.common

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
import one.mixin.android.api.request.AuthorizeRequest
import one.mixin.android.api.request.CollectibleRequest
import one.mixin.android.api.request.ConversationCircleRequest
import one.mixin.android.api.request.ConversationRequest
import one.mixin.android.api.request.ParticipantRequest
import one.mixin.android.api.request.PinRequest
import one.mixin.android.api.request.RawTransactionsRequest
import one.mixin.android.api.request.RelationshipRequest
import one.mixin.android.api.request.TransferRequest
import one.mixin.android.api.request.WithdrawalRequest
import one.mixin.android.api.response.AuthorizationResponse
import one.mixin.android.api.response.ConversationResponse
import one.mixin.android.extension.escapeSql
import one.mixin.android.job.ConversationJob
import one.mixin.android.job.GenerateAvatarJob
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshConversationJob
import one.mixin.android.job.RefreshUserJob
import one.mixin.android.job.UpdateRelationshipJob
import one.mixin.android.repository.AccountRepository
import one.mixin.android.repository.AssetRepository
import one.mixin.android.repository.ConversationRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.session.Session
import one.mixin.android.session.encryptPin
import one.mixin.android.ui.common.message.CleanMessageHelper
import one.mixin.android.vo.Account
import one.mixin.android.vo.Address
import one.mixin.android.vo.App
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.Circle
import one.mixin.android.vo.CircleConversation
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.ConversationCircleManagerItem
import one.mixin.android.vo.Snapshot
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.Trace
import one.mixin.android.vo.User
import one.mixin.android.vo.generateConversationId
import one.mixin.android.vo.giphy.Gif
import one.mixin.android.vo.toSimpleChat
import java.io.File
import javax.inject.Inject

@HiltViewModel
class BottomSheetViewModel @Inject internal constructor(
    private val accountRepository: AccountRepository,
    private val jobManager: MixinJobManager,
    private val userRepository: UserRepository,
    private val assetRepository: AssetRepository,
    private val conversationRepo: ConversationRepository,
    private val cleanMessageHelper: CleanMessageHelper
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
        assetRepository.simpleAssetsWithBalance()
    }

    suspend fun transfer(
        assetId: String,
        userId: String,
        amount: String,
        code: String,
        trace: String?,
        memo: String?
    ) =
        assetRepository.transfer(
            TransferRequest(
                assetId,
                userId,
                amount,
                encryptPin(Session.getPinToken()!!, code),
                trace,
                memo
            )
        )

    fun authorize(request: AuthorizeRequest): Observable<MixinResponse<AuthorizationResponse>> =
        accountRepository.authorize(request).subscribeOn(Schedulers.io()).observeOn(
            AndroidSchedulers.mainThread()
        )

    suspend fun paySuspend(request: TransferRequest) = withContext(Dispatchers.IO) {
        assetRepository.paySuspend(request)
    }

    suspend fun withdrawal(
        addressId: String,
        amount: String,
        code: String,
        traceId: String,
        memo: String?,
        fee: String?,
    ) =
        assetRepository.withdrawal(
            WithdrawalRequest(
                addressId,
                amount,
                encryptPin(Session.getPinToken()!!, code)!!,
                traceId,
                memo,
                fee,
            )
        )

    suspend fun syncAddr(
        assetId: String,
        destination: String?,
        label: String?,
        tag: String?,
        code: String
    ): MixinResponse<Address> =
        assetRepository.syncAddr(
            AddressRequest(
                assetId,
                destination,
                tag,
                label,
                encryptPin(Session.getPinToken()!!, code)!!
            )
        )

    suspend fun saveAddr(addr: Address) = withContext(Dispatchers.IO) {
        assetRepository.saveAddr(addr)
    }

    suspend fun deleteAddr(id: String, code: String): MixinResponse<Unit> =
        assetRepository.deleteAddr(id, encryptPin(Session.getPinToken()!!, code)!!)

    suspend fun deleteLocalAddr(id: String) = assetRepository.deleteLocalAddr(id)

    suspend fun simpleAssetItem(id: String) = assetRepository.simpleAssetItem(id)

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
        recipientId: String? = null
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
                participants = listOf(participantRequest)
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
                type = ConversationJob.TYPE_EXIT
            )
        )
    }

    fun updateGroup(
        conversationId: String,
        name: String? = null,
        iconBase64: String? = null,
        announcement: String? = null
    ) {
        val request = ConversationRequest(
            conversationId,
            name = name,
            iconBase64 = iconBase64,
            announcement = announcement
        )
        jobManager.addJobInBackground(
            ConversationJob(
                conversationId = conversationId,
                request = request,
                type = ConversationJob.TYPE_UPDATE
            )
        )
    }

    fun refreshUser(userId: String, forceRefresh: Boolean) {
        jobManager.addJobInBackground(RefreshUserJob(listOf(userId), forceRefresh = forceRefresh))
    }

    fun refreshUsers(userIds: List<String>, conversationId: String?, conversationAvatarUserIds: List<String>?) {
        jobManager.addJobInBackground(
            RefreshUserJob(userIds, conversationId)
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
        val address = assetRepository.findAddressById(addressId, assetId)
            ?: return@withContext assetRepository.refreshAndGetAddress(addressId, assetId)
        return@withContext Pair(address, false)
    }

    suspend fun findAssetItemById(assetId: String): AssetItem? =
        assetRepository.findAssetItemById(assetId)

    suspend fun refreshAsset(assetId: String): AssetItem? {
        return withContext(Dispatchers.IO) {
            assetRepository.findOrSyncAsset(assetId)
        }
    }

    suspend fun refreshSnapshot(snapshotId: String): SnapshotItem? {
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
                            val snapshotItem = assetRepository.findSnapshotById(snapshot.snapshotId)
                                ?: return@handleMixinResponse null
                            return@handleMixinResponse Pair(snapshotItem, assetItem)
                        }
                    }
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

    suspend fun preferences(request: AccountUpdateRequest) = withContext(Dispatchers.IO) {
        accountRepository.preferences(request)
    }

    suspend fun searchAppByHost(query: String): List<App> {
        val escapedQuery = query.trim().escapeSql()
        return userRepository.searchAppByHost(escapedQuery)
    }

    suspend fun findMultiUsers(
        senders: Array<String>,
        receivers: Array<String>
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
                }
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
            PinRequest(encryptPin(Session.getPinToken()!!, pin)!!)
        )

    suspend fun unlockMultisigs(requestId: String, pin: String) =
        accountRepository.unlockMultisigs(
            requestId,
            PinRequest(encryptPin(Session.getPinToken()!!, pin)!!)
        )

    suspend fun cancelMultisigs(requestId: String) = withContext(Dispatchers.IO) {
        accountRepository.cancelMultisigs(requestId)
    }

    suspend fun getToken(tokenId: String) = accountRepository.getToken(tokenId)

    suspend fun signCollectibleTransfer(requestId: String, pinRequest: CollectibleRequest) = accountRepository.signCollectibleTransfer(requestId, pinRequest)

    suspend fun unlockCollectibleTransfer(requestId: String, pinRequest: CollectibleRequest) = accountRepository.unlockCollectibleTransfer(requestId, pinRequest)

    suspend fun cancelCollectibleTransfer(requestId: String) = accountRepository.cancelCollectibleTransfer(requestId)

    suspend fun transactions(
        rawTransactionsRequest: RawTransactionsRequest,
        pin: String
    ): MixinResponse<Void> {
        rawTransactionsRequest.pin = encryptPin(Session.getPinToken()!!, pin)!!
        return accountRepository.transactions(rawTransactionsRequest)
    }

    suspend fun findSnapshotById(snapshotId: String) = assetRepository.findSnapshotById(snapshotId)

    fun insertSnapshot(snapshot: Snapshot) = viewModelScope.launch(Dispatchers.IO) {
        assetRepository.insertSnapshot(snapshot)
    }

    fun update(request: AccountUpdateRequest): Observable<MixinResponse<Account>> =
        accountRepository.update(request).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

    fun insertUser(user: User) = viewModelScope.launch(Dispatchers.IO) {
        userRepository.upsert(user)
    }

    suspend fun errorCount() = accountRepository.errorCount()

    suspend fun loadFavoriteApps(userId: String, loadAction: (List<App>?) -> Unit) {
        withContext(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                loadAction(
                    accountRepository.getFavoriteAppsByUserId(
                        userId
                    )
                )
            }
            handleMixinResponse(
                invokeNetwork = { accountRepository.getUserFavoriteApps(userId) },
                successBlock = {
                    it.data?.let { data ->
                        accountRepository.insertFavoriteApps(userId, data)
                        refreshAppNotExist(data.map { app -> app.appId })
                        withContext(Dispatchers.Main) {
                            loadAction(accountRepository.getFavoriteAppsByUserId(userId))
                        }
                    }
                },
                exceptionBlock = {
                    return@handleMixinResponse true
                }
            )
        }
    }

    private suspend fun refreshAppNotExist(appIds: List<String>) = withContext(Dispatchers.IO) {
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

    suspend fun insertTrace(trace: Trace) = assetRepository.insertTrace(trace)

    suspend fun suspendFindTraceById(traceId: String) = assetRepository.suspendFindTraceById(traceId)

    suspend fun findLatestTrace(opponentId: String?, destination: String?, tag: String?, amount: String, assetId: String) =
        assetRepository.findLatestTrace(opponentId, destination, tag, amount, assetId)

    suspend fun deletePreviousTraces() = assetRepository.deletePreviousTraces()

    suspend fun suspendDeleteTraceById(traceId: String) = assetRepository.suspendDeleteTraceById(traceId)

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
            }
        )
    }

    suspend fun findSameConversations(selfId: String, userId: String) = conversationRepo.findSameConversations(selfId, userId)
}
