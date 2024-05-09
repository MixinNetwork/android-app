package one.mixin.android.ui.search

import android.os.CancellationSignal
import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Config
import androidx.paging.PagedList
import androidx.paging.toLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants.PAGE_SIZE
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.ConversationRequest
import one.mixin.android.api.request.ParticipantRequest
import one.mixin.android.api.response.ConversationResponse
import one.mixin.android.extension.escapeSql
import one.mixin.android.extension.pmap
import one.mixin.android.job.MixinJobManager
import one.mixin.android.repository.AccountRepository
import one.mixin.android.repository.ConversationRepository
import one.mixin.android.repository.TokenRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.ui.common.message.CleanMessageHelper
import one.mixin.android.util.ControlledRunner
import one.mixin.android.util.mlkit.firstUrl
import one.mixin.android.vo.ChatMinimal
import one.mixin.android.vo.Conversation
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.InscriptionItem
import one.mixin.android.vo.SearchMessageDetailItem
import one.mixin.android.vo.SearchMessageItem
import one.mixin.android.vo.User
import one.mixin.android.vo.generateConversationId
import one.mixin.android.vo.safe.SafeInscription
import one.mixin.android.vo.safe.TokenItem
import javax.inject.Inject

@HiltViewModel
class SearchViewModel
    @Inject
    internal constructor(
        val userRepository: UserRepository,
        val conversationRepository: ConversationRepository,
        val tokenRepository: TokenRepository,
        val accountRepository: AccountRepository,
        val jobManager: MixinJobManager,
        val cleanMessageHelper: CleanMessageHelper,
    ) : ViewModel() {
        val messageControlledRunner = ControlledRunner<List<SearchMessageItem>?>()

        fun findConversationById(conversationId: String): Observable<Conversation> =
            conversationRepository.findConversationById(conversationId)
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

        suspend inline fun fuzzySearchUrl(query: String?): String? {
            return if (query.isNullOrEmpty()) {
                null
            } else {
                firstUrl(query)
            }
        }

        suspend inline fun <reified T> fuzzySearch(
            cancellationSignal: CancellationSignal,
            query: String?,
            limit: Int = -1,
        ): List<Parcelable>? =
            if (query.isNullOrBlank()) {
                null
            } else {
                val escapedQuery = query.trim().escapeSql()
                when (T::class) {
                    TokenItem::class ->
                        tokenRepository.fuzzySearchToken(
                            escapedQuery,
                            cancellationSignal,
                        )
                    User::class -> userRepository.fuzzySearchUser(escapedQuery, cancellationSignal)
                    ChatMinimal::class ->
                        conversationRepository.fuzzySearchChat(
                            escapedQuery,
                            cancellationSignal,
                        )
                    else ->
                        messageControlledRunner.cancelPreviousThenRun {
                            conversationRepository.fuzzySearchMessage(
                                escapedQuery,
                                limit,
                                cancellationSignal,
                            )
                        }
                }
            }

        suspend fun fuzzyBots(
            cancellationSignal: CancellationSignal,
            query: String?,
        ): List<User>? {
            return if (query.isNullOrBlank()) {
                null
            } else {
                val escapedQuery = query.trim().escapeSql()
                userRepository.fuzzySearchBots(escapedQuery, cancellationSignal)
            }
        }

        suspend fun fuzzyInscription(cancellationSignal: CancellationSignal, query: String?): List<SafeInscription>? {
            return if (query.isNullOrBlank()) {
                null
            } else {
                val escapedQuery = query.trim().escapeSql()
                tokenRepository.fuzzyInscription(escapedQuery, cancellationSignal)
            }
        }

        fun findAppsByIds(appIds: List<String>) = userRepository.findAppsByIds(appIds)

        suspend fun findBotsByIds(appIds: Set<String>) = userRepository.findBotsByIds(appIds)

        fun observeFuzzySearchMessageDetail(
            query: String,
            conversationId: String,
            cancellationSignal: CancellationSignal,
        ): LiveData<PagedList<SearchMessageDetailItem>> {
            val escapedQuery = query.trim().escapeSql()
            return conversationRepository.fuzzySearchMessageDetail(
                escapedQuery,
                conversationId,
                cancellationSignal,
            ).toLiveData(
                config =
                    Config(
                        pageSize = PAGE_SIZE,
                        prefetchDistance = PAGE_SIZE * 2,
                        enablePlaceholders = false,
                    ),
            )
        }

        fun search(query: String): Observable<MixinResponse<User>> =
            accountRepository.search(query).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())

        fun insertUser(user: User) =
            viewModelScope.launch(Dispatchers.IO) {
                userRepository.upsert(user)
            }

        suspend fun findMessageIndex(
            conversationId: String,
            messageId: String,
        ) =
            conversationRepository.findMessageIndex(conversationId, messageId)

        fun updateConversationPinTimeById(
            conversationId: String,
            circleId: String?,
            pinTime: String?,
            callback: () -> Unit,
        ) =
            viewModelScope.launch {
                conversationRepository.updateConversationPinTimeById(conversationId, circleId, pinTime)
                callback.invoke()
            }

        fun deleteConversation(
            conversationId: String,
            callback: () -> Unit,
        ) =
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    cleanMessageHelper.deleteMessageByConversationId(conversationId, true)
                }
                callback.invoke()
            }

        suspend fun mute(
            duration: Long,
            conversationId: String? = null,
            senderId: String? = null,
            recipientId: String? = null,
        ): MixinResponse<ConversationResponse> {
            require(conversationId != null || (senderId != null && recipientId != null)) {
                "error data"
            }
            if (conversationId != null) {
                val request =
                    ConversationRequest(
                        conversationId,
                        ConversationCategory.GROUP.name,
                        duration = duration,
                    )
                return conversationRepository.muteSuspend(conversationId, request)
            } else {
                var cid = conversationRepository.getConversationIdIfExistsSync(recipientId!!)
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
                return conversationRepository.muteSuspend(cid, request)
            }
        }

        suspend fun updateGroupMuteUntil(
            conversationId: String,
            muteUntil: String,
        ) {
            withContext(Dispatchers.IO) {
                conversationRepository.updateGroupMuteUntil(conversationId, muteUntil)
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

        suspend fun queryAssets(assetIds: List<String>): List<TokenItem> =
            assetIds.pmap {
                tokenRepository.syncAsset(it)
            }.filterNotNull()

        suspend fun findUserByAppId(appId: String) = userRepository.findUserByAppId(appId)

}
