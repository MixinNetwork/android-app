package one.mixin.android.ui.common

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.AccountUpdateRequest
import one.mixin.android.api.request.AddressRequest
import one.mixin.android.api.request.AuthorizeRequest
import one.mixin.android.api.request.ConversationRequest
import one.mixin.android.api.request.ParticipantRequest
import one.mixin.android.api.request.RelationshipRequest
import one.mixin.android.api.request.TransferRequest
import one.mixin.android.api.request.WithdrawalRequest
import one.mixin.android.api.response.AuthorizationResponse
import one.mixin.android.api.response.ConversationResponse
import one.mixin.android.api.response.PaymentResponse
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
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.Snapshot
import one.mixin.android.vo.User
import one.mixin.android.vo.generateConversationId
import one.mixin.android.vo.giphy.Gif
import org.jetbrains.anko.doAsync

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

    fun simpleAssetsWithBalance() = assetRepository.simpleAssetsWithBalance()

    fun transfer(assetId: String, userId: String, amount: String, code: String, trace: String?, memo: String?) =
        assetRepository.transfer(TransferRequest(assetId, userId, amount, encryptPin(Session.getPinToken()!!, code), trace, memo))
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())!!

    fun authorize(request: AuthorizeRequest): Observable<MixinResponse<AuthorizationResponse>> =
        accountRepository.authorize(request).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

    fun pay(request: TransferRequest): Observable<MixinResponse<PaymentResponse>> =
        assetRepository.pay(request).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

    fun withdrawal(addressId: String, amount: String, code: String, traceId: String, memo: String?):
        Observable<MixinResponse<Snapshot>> =
        Observable.just(Session.getPinToken()).map { pinToken ->
            assetRepository.withdrawal(
                WithdrawalRequest(addressId, amount, encryptPin(pinToken, code)!!, traceId, memo))
                .execute().body()!!
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

    fun insertSnapshot(snapshot: Snapshot) {
        doAsync {
            assetRepository.insertSnapshot(snapshot)
        }
    }

    fun syncAddr(assetId: String, publicKey: String?, label: String?, code: String, accountName: String?, accountTag: String?): Observable<MixinResponse<Address>> =
        assetRepository.syncAddr(AddressRequest(assetId, publicKey, label, encryptPin(Session.getPinToken()!!, code)!!, accountName, accountTag))
            .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

    fun saveAddr(addr: Address) = assetRepository.saveAddr(addr)

    fun deleteAddr(id: String, code: String): Observable<MixinResponse<Unit>> =
        assetRepository.deleteAddr(id, encryptPin(Session.getPinToken()!!, code)!!).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

    fun deleteLocalAddr(id: String) = assetRepository.deleteLocalAddr(id)

    fun simpleAssetItem(id: String) = assetRepository.simpleAssetItem(id)

    fun findAssetItem(id: String) =
        Single.fromCallable {
            assetRepository.simpleAssetItem(id)
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

    fun findUserById(id: String): LiveData<User> = userRepository.findUserById(id)

    fun updateRelationship(request: RelationshipRequest, deleteConversationId: String? = null) {
        jobManager.addJobInBackground(UpdateRelationshipJob(request, deleteConversationId))
    }

    fun getLimitParticipants(conversationId: String, limit: Int) = conversationRepo.getLimitParticipants(conversationId, limit)

    fun getParticipantsCount(conversationId: String) = conversationRepo.getParticipantsCount(conversationId)

    fun getConversationById(id: String) = conversationRepo.getConversationById(id)

    fun getConversation(id: String) = conversationRepo.getConversation(id)

    fun findParticipantByIds(conversationId: String, userId: String) = conversationRepo.findParticipantByIds(conversationId, userId)

    fun mute(senderId: String, recipientId: String, duration: Long) {
        viewModelScope.launch(SINGLE_DB_THREAD) {
            var conversationId = conversationRepo.getConversationIdIfExistsSync(recipientId)
            if (conversationId == null) {
                conversationId = generateConversationId(senderId, recipientId)
            }
            val participantRequest = ParticipantRequest(recipientId, "")
            jobManager.addJobInBackground(ConversationJob(ConversationRequest(conversationId,
                ConversationCategory.CONTACT.name, duration = duration, participants = listOf(participantRequest)),
                recipientId = recipientId, type = ConversationJob.TYPE_MUTE))
        }
    }

    fun mute(conversationId: String, duration: Long) {
        jobManager.addJobInBackground(ConversationJob(conversationId = conversationId,
            request = ConversationRequest(conversationId, ConversationCategory.GROUP.name, duration = duration),
            type = ConversationJob.TYPE_MUTE))
    }

    suspend fun findAppById(id: String) = userRepository.findAppById(id)

    fun getUserById(id: String) = userRepository.getUserById(id)

    fun getUser(id: String) = userRepository.getUser(id)

    fun startGenerateAvatar(conversationId: String) {
        jobManager.addJobInBackground(GenerateAvatarJob(conversationId))
    }

    fun deleteMessageByConversationId(conversationId: String) {
        conversationRepo.deleteMessageByConversationId(conversationId)
    }

    fun exitGroup(conversationId: String) {
        jobManager.addJobInBackground(ConversationJob(conversationId = conversationId, type = ConversationJob.TYPE_EXIT))
    }

    fun deleteGroup(conversationId: String) {
        conversationRepo.deleteConversationById(conversationId)
    }

    fun updateGroup(
        conversationId: String,
        name: String? = null,
        iconBase64: String? = null,
        announcement: String? = null
    ) {
        val request = ConversationRequest(conversationId, name = name,
            iconBase64 = iconBase64, announcement = announcement)
        jobManager.addJobInBackground(ConversationJob(conversationId = conversationId,
            request = request, type = ConversationJob.TYPE_UPDATE))
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
        viewModelScope.async(Dispatchers.IO) { assetRepository.findAddressById(addressId, assetId) }.await()

    suspend fun findAssetItemById(assetId: String): AssetItem? =
        viewModelScope.async(Dispatchers.IO) {
            return@async assetRepository.findAssetItemById(assetId)
        }.await()

    suspend fun refreshAsset(assetId: String): AssetItem? {
        return viewModelScope.async(Dispatchers.IO) {
            val response = assetRepository.asset(assetId).execute().body()
            if (response != null && response.isSuccess && response.data != null) {
                response.data?.let {
                    assetRepository.upsert(it)
                    return@async assetRepository.findAssetItemById(assetId)
                }
            }
            null
        }.await()
    }

    suspend fun getFiats() = accountRepository.getFiats()

    suspend fun preferences(request: AccountUpdateRequest) = accountRepository.preferences(request)
}
