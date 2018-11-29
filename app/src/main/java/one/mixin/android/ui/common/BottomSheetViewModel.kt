package one.mixin.android.ui.common

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.work.WorkManager
import androidx.work.workDataOf
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import one.mixin.android.api.MixinResponse
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
import one.mixin.android.extension.enqueueOneTimeNetworkWorkRequest
import one.mixin.android.job.ConversationJob
import one.mixin.android.job.GenerateAvatarJob
import one.mixin.android.job.MixinJobManager
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
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.Snapshot
import one.mixin.android.vo.User
import one.mixin.android.vo.generateConversationId
import one.mixin.android.vo.giphy.Gif
import one.mixin.android.work.RefreshConversationWorker
import one.mixin.android.work.RefreshUserWorker
import org.jetbrains.anko.doAsync
import javax.inject.Inject

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
        WorkManager.getInstance().enqueueOneTimeNetworkWorkRequest<RefreshConversationWorker>(
            workDataOf(RefreshConversationWorker.CONVERSATION_ID to conversationId))
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

    fun findUserById(id: String): LiveData<User> = userRepository.findUserById(id)

    fun updateRelationship(request: RelationshipRequest) {
        jobManager.addJobInBackground(UpdateRelationshipJob(request))
    }

    fun getLimitParticipants(conversationId: String, limit: Int) = conversationRepo.getLimitParticipants(conversationId, limit)

    fun getParticipantsCount(conversationId: String) = conversationRepo.getParticipantsCount(conversationId)

    fun getConversationById(id: String) = conversationRepo.getConversationById(id)

    fun findParticipantByIds(conversationId: String, userId: String) = conversationRepo.findParticipantByIds(conversationId, userId)

    fun mute(senderId: String, recipientId: String, duration: Long) {
        GlobalScope.launch(SINGLE_DB_THREAD) {
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

    fun findAppById(id: String) = userRepository.findAppById(id)

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

    fun refreshUser(userId: String) {
        WorkManager.getInstance().enqueueOneTimeNetworkWorkRequest<RefreshUserWorker>(
            workDataOf(RefreshUserWorker.USER_IDS to arrayOf(userId)))
    }

    fun verifyPin(code: String): Observable<MixinResponse<Account>> = accountRepository.verifyPin(code)

    fun trendingGifs(limit: Int, offset: Int): Observable<List<Gif>> =
        accountRepository.trendingGifs(limit, offset).map { it.data }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())

    fun searchGifs(query: String, limit: Int, offset: Int): Observable<List<Gif>> =
        accountRepository.searchGifs(query, limit, offset).map { it.data }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
}