package one.mixin.android.ui.common

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.AccountUpdateRequest
import one.mixin.android.api.request.AddressRequest
import one.mixin.android.api.request.AuthorizeRequest
import one.mixin.android.api.request.ConversationRequest
import one.mixin.android.api.request.ParticipantRequest
import one.mixin.android.api.request.PinRequest
import one.mixin.android.api.request.RawTransactionsRequest
import one.mixin.android.api.request.RelationshipRequest
import one.mixin.android.api.request.TransferRequest
import one.mixin.android.api.request.WithdrawalRequest
import one.mixin.android.api.response.AuthorizationResponse
import one.mixin.android.api.response.ConversationResponse
import one.mixin.android.api.response.PaymentResponse
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
import one.mixin.android.util.SINGLE_DB_THREAD
import one.mixin.android.util.Session
import one.mixin.android.util.encryptPin
import one.mixin.android.vo.Account
import one.mixin.android.vo.Address
import one.mixin.android.vo.App
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.Snapshot
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.User
import one.mixin.android.vo.generateConversationId
import one.mixin.android.vo.giphy.Gif

class BottomSheetViewModel @Inject internal constructor(
    private val accountRepository: AccountRepository,
    private val jobManager: MixinJobManager,
    private val userRepository: UserRepository,
    private val assetRepository: AssetRepository,
    private val conversationRepo: ConversationRepository
) : ViewModel() {
    fun searchCode(code: String): Observable<Pair<String, Any>> {
        return accountRepository.searchCode(code).observeOn(AndroidSchedulers.mainThread())
    }

    fun join(code: String): Observable<MixinResponse<ConversationResponse>> =
        accountRepository.join(code).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

    fun refreshConversation(conversationId: String) {
        jobManager.addJobInBackground(RefreshConversationJob(conversationId))
    }

    suspend fun simpleAssetsWithBalance() = assetRepository.simpleAssetsWithBalance()

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

    fun pay(request: TransferRequest): Observable<MixinResponse<PaymentResponse>> =
        assetRepository.pay(request).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

    suspend fun withdrawal(
        addressId: String,
        amount: String,
        code: String,
        traceId: String,
        memo: String?
    ) =
        assetRepository.withdrawal(
            WithdrawalRequest(
                addressId,
                amount,
                encryptPin(Session.getPinToken()!!, code)!!,
                traceId,
                memo
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

    fun updateRelationship(request: RelationshipRequest, deleteConversationId: String? = null) {
        jobManager.addJobInBackground(UpdateRelationshipJob(request, deleteConversationId))
    }

    fun getParticipantsCount(conversationId: String) =
        conversationRepo.getParticipantsCount(conversationId)

    fun getConversationById(id: String) = conversationRepo.getConversationById(id)

    fun getConversation(id: String) = conversationRepo.getConversation(id)

    fun findParticipantByIds(conversationId: String, userId: String) =
        conversationRepo.findParticipantByIds(conversationId, userId)

    fun mute(senderId: String, recipientId: String, duration: Long) {
        viewModelScope.launch(SINGLE_DB_THREAD) {
            var conversationId = conversationRepo.getConversationIdIfExistsSync(recipientId)
            if (conversationId == null) {
                conversationId = generateConversationId(senderId, recipientId)
            }
            val participantRequest = ParticipantRequest(recipientId, "")
            jobManager.addJobInBackground(
                ConversationJob(
                    ConversationRequest(
                        conversationId,
                        ConversationCategory.CONTACT.name,
                        duration = duration,
                        participants = listOf(participantRequest)
                    ),
                    recipientId = recipientId, type = ConversationJob.TYPE_MUTE
                )
            )
        }
    }

    fun mute(conversationId: String, duration: Long) {
        jobManager.addJobInBackground(
            ConversationJob(
                conversationId = conversationId,
                request = ConversationRequest(
                    conversationId,
                    ConversationCategory.GROUP.name,
                    duration = duration
                ),
                type = ConversationJob.TYPE_MUTE
            )
        )
    }

    suspend fun findAppById(id: String) = userRepository.findAppById(id)

    suspend fun getAppAndCheckUser(userId: String) = userRepository.getAppAndCheckUser(userId)

    fun getUserById(id: String) = userRepository.getUserById(id)

    fun getUser(id: String) = userRepository.getUser(id)

    fun startGenerateAvatar(conversationId: String) {
        jobManager.addJobInBackground(GenerateAvatarJob(conversationId))
    }

    fun deleteMessageByConversationId(conversationId: String) = viewModelScope.launch {
        conversationRepo.deleteMessageByConversationId(conversationId)
    }

    fun exitGroup(conversationId: String) {
        jobManager.addJobInBackground(
            ConversationJob(
                conversationId = conversationId,
                type = ConversationJob.TYPE_EXIT
            )
        )
    }

    fun deleteGroup(conversationId: String) = viewModelScope.launch {
        conversationRepo.deleteConversationById(conversationId)
    }

    fun updateGroup(
        conversationId: String,
        name: String? = null,
        iconBase64: String? = null,
        announcement: String? = null
    ) {
        val request = ConversationRequest(
            conversationId, name = name,
            iconBase64 = iconBase64, announcement = announcement
        )
        jobManager.addJobInBackground(
            ConversationJob(
                conversationId = conversationId,
                request = request, type = ConversationJob.TYPE_UPDATE
            )
        )
    }

    fun refreshUser(userId: String, forceRefresh: Boolean) {
        jobManager.addJobInBackground(RefreshUserJob(listOf(userId), forceRefresh = forceRefresh))
    }

    suspend fun verifyPin(code: String): MixinResponse<Account> = accountRepository.verifyPin(code)

    fun trendingGifs(limit: Int, offset: Int): Observable<List<Gif>> =
        accountRepository.trendingGifs(limit, offset).map { it.data }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())

    fun searchGifs(query: String, limit: Int, offset: Int): Observable<List<Gif>> =
        accountRepository.searchGifs(query, limit, offset).map { it.data }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())

    fun logoutAsync(sessionId: String) = accountRepository.logoutAsync(sessionId)

    suspend fun findAddressById(addressId: String, assetId: String) =
        viewModelScope.async(Dispatchers.IO) {
            assetRepository.findAddressById(
                addressId,
                assetId
            )
        }.await()

    suspend fun findAssetItemById(assetId: String): AssetItem? =
        viewModelScope.async(Dispatchers.IO) {
            return@async assetRepository.findAssetItemById(assetId)
        }.await()

    suspend fun refreshAsset(assetId: String): AssetItem? {
        return withContext(Dispatchers.IO) {
            var result: AssetItem? = null
            handleMixinResponse(
                invokeNetwork = {
                    assetRepository.asset(assetId)
                },
                successBlock = { response ->
                    response.data?.let {
                        assetRepository.insert(it)
                        result = assetRepository.findAssetItemById(assetId)
                    }
                }
            )
            result
        }
    }

    suspend fun refreshSnapshot(snapshotId: String): SnapshotItem? {
        return withContext(Dispatchers.IO) {
            var result: SnapshotItem? = null
            handleMixinResponse(
                invokeNetwork = {
                    assetRepository.getSnapshotById(snapshotId)
                },
                successBlock = { response ->
                    response.data?.let {
                        assetRepository.insertSnapshot(it)
                        result = assetRepository.findSnapshotById(snapshotId)
                    }
                }
            )
            result
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
                        assetRepository.getSnapshotByTraceId(traceId)
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

    suspend fun preferences(request: AccountUpdateRequest) = accountRepository.preferences(request)

    suspend fun searchAppByHost(query: String): List<App> {
        val escapedQuery = query.trim().escapeSql()
        return userRepository.searchAppByHost(escapedQuery)
    }

    suspend fun findMultiUsers(
        senders: Array<String>,
        receivers: Array<String>
    ): List<User> {
        val userIds = mutableSetOf<String>().apply {
            addAll(senders)
            addAll(receivers)
        }
        val existUserIds = userRepository.findUserExist(userIds.toList())
        val queryUsers = userIds.filter {
            !existUserIds.contains(it)
        }
        if (queryUsers.isNotEmpty()) {
            return handleMixinResponse(
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
            return userRepository.findMultiUsersByIds(userIds)
        }
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

    suspend fun cancelMultisigs(requestId: String) =
        accountRepository.cancelMultisigs(requestId)

    suspend fun transactions(
        rawTransactionsRequest: RawTransactionsRequest,
        pin: String
    ): MixinResponse<Void> {
        rawTransactionsRequest.pin = encryptPin(Session.getPinToken()!!, pin)!!
        return accountRepository.transactions(rawTransactionsRequest)
    }

    suspend fun findSnapshotById(snapshotId: String) = assetRepository.findSnapshotById(snapshotId)

    suspend fun getSnapshotById(snapshotId: String) = assetRepository.getSnapshotById(snapshotId)

    fun insertSnapshot(snapshot: Snapshot) = assetRepository.insertSnapshot(snapshot)

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
                }
            )
        }
    }

    private suspend fun refreshAppNotExist(appIds: List<String>) = withContext(Dispatchers.IO) {
        accountRepository.refreshAppNotExist(appIds)
    }
}
