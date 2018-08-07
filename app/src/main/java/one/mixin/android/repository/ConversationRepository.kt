package one.mixin.android.repository

import android.arch.lifecycle.LiveData
import android.arch.paging.DataSource
import io.reactivex.Observable
import one.mixin.android.AppExecutors
import one.mixin.android.api.request.ConversationRequest
import one.mixin.android.api.service.ConversationService
import one.mixin.android.db.AppDao
import one.mixin.android.db.ConversationDao
import one.mixin.android.db.MessageDao
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.ParticipantDao
import one.mixin.android.db.insertConversation
import one.mixin.android.vo.Conversation
import one.mixin.android.vo.ConversationItem
import one.mixin.android.vo.ConversationItemMinimal
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.Participant
import one.mixin.android.vo.SearchMessageItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepository
@Inject
internal constructor(
    private val messageDao: MessageDao,
    private val conversationDao: ConversationDao,
    private val participantDao: ParticipantDao,
    private val appDao: AppDao,
    private val appExecutors: AppExecutors,
    private val appDatabase: MixinDatabase,
    private val conversationService: ConversationService
) {

    fun conversation(): LiveData<List<ConversationItem>> = conversationDao.conversationList()

    fun insertConversation(conversation: Conversation, participants: List<Participant>) {
        appExecutors.diskIO().execute {
            appDatabase.runInTransaction {
                conversationDao.insertConversation(conversation)
                participantDao.insertList(participants)
            }
        }
    }

    fun syncInsertConversation(conversation: Conversation, participants: List<Participant>) {
        appDatabase.runInTransaction {
            conversationDao.insertConversation(conversation)
            participantDao.insertList(participants)
        }
    }

    fun getConversationById(conversationId: String): LiveData<Conversation> =
        conversationDao.getConversationById(conversationId)

    fun findConversationById(conversationId: String): Observable<Conversation> = Observable.just(conversationId).map {
        conversationDao.findConversationById(conversationId)
    }

    fun searchConversationById(conversationId: String) = conversationDao.searchConversationById(conversationId)

    fun findMessageById(messageId: String) = messageDao.findMessageById(messageId)

    fun saveDraft(conversationId: String, draft: String) {
        appExecutors.diskIO().execute {
            conversationDao.saveDraft(conversationId, draft)
        }
    }

    fun getConversation(conversationId: String) = conversationDao.getConversation(conversationId)

    fun fuzzySearchMessage(query: String): List<SearchMessageItem> = messageDao.fuzzySearchMessage(query)

    fun fuzzySearchGroup(query: String): List<ConversationItemMinimal> = conversationDao.fuzzySearchGroup(query)

    fun getMessages(conversationId: String): DataSource.Factory<Int, MessageItem> =
        messageDao.getMessages(conversationId)

    fun indexUnread(conversationId: String) = conversationDao.indexUnread(conversationId)

    fun getMediaMessages(conversationId: String): List<MessageItem> =
        messageDao.getMediaMessages(conversationId)

    fun getConversationIdIfExistsSync(recipientId: String) = conversationDao.getConversationIdIfExistsSync(recipientId)

    fun makeMessageReadByConversationId(conversationId: String, accountId: String, messageId: String) {
        appExecutors.diskIO().execute {
            messageDao.makeMessageReadByConversationId(conversationId, accountId, messageId)
        }
    }

    fun makeMessageReadByConversationIdSync(conversationId: String, accountId: String, messageId: String) {
        messageDao.makeMessageReadByConversationId(conversationId, accountId, messageId)
    }

    fun getUnreadMessage(conversationId: String, accountId: String, messageId: String): List<String> {
        return messageDao.getUnreadMessage(conversationId, accountId, messageId)
    }

    fun updateLastReadMessageId(conversationId: String, messageId: String) {
        conversationDao.updateLastReadMessageId(conversationId, messageId)
    }

    fun updateCodeUrl(conversationId: String, codeUrl: String) {
        appExecutors.diskIO().execute {
            conversationDao.updateCodeUrl(conversationId, codeUrl)
        }
    }

    fun getGroupParticipants(conversationId: String) = participantDao.getParticipants(conversationId)

    fun getGroupParticipantsLiveData(conversationId: String) =
        participantDao.getGroupParticipantsLiveData(conversationId)

    fun updateMediaStatusStatus(status: String, messageId: String) = messageDao.updateMediaStatus(status, messageId)

    fun findUnreadMessages(conversationId: String) = messageDao.findUnreadMessages(conversationId)

    fun deleteMessage(id: String) = messageDao.deleteMessage(id)
    fun deleteConversationById(conversationId: String) {
        appExecutors.diskIO().execute {
            conversationDao.deleteConversationById(conversationId)
        }
    }

    fun updateConversationPinTimeById(conversationId: String, pinTime: String?) {
        appExecutors.diskIO().execute {
            conversationDao.updateConversationPinTimeById(conversationId, pinTime)
        }
    }

    fun deleteMessageByConversationId(conversationId: String) {
        appExecutors.diskIO().execute {
            messageDao.deleteMessageByConversationId(conversationId)
        }
    }

    fun getRealParticipants(conversationId: String) = participantDao.getRealParticipants(conversationId)

    fun getGroupConversationApp(conversationId: String) = appDao.getGroupConversationApp(conversationId)

    fun getConversationApp(userId: String?) = appDao.getConversationApp(userId)

    fun updateAsync(conversationId: String, request: ConversationRequest) =
        conversationService.updateAsync(conversationId, request)

    fun updateAnnouncement(conversationId: String, announcement: String) {
        appExecutors.diskIO().execute {
            conversationDao.updateConversationAnnouncement(conversationId, announcement)
        }
    }

    fun getLimitParticipants(conversationId: String, limit: Int) = participantDao.getLimitParticipants(conversationId, limit)

    fun findParticipantByIds(conversationId: String, userId: String) = participantDao.findParticipantByIds(conversationId, userId)

    fun getParticipantsCount(conversationId: String) = participantDao.getParticipantsCount(conversationId)

    fun getStorageUsage(conversationId: String) = conversationDao.getStorageUsage(conversationId)

    fun getConversationStorageUsage() = conversationDao.getConversationStorageUsage()

    fun getMediaByConversationIdAndCategory(conversationId: String, category: String) = messageDao.getMediaByConversationIdAndCategory(conversationId, category)

    fun findMessageIndex(conversationId: String, messageId: String) = messageDao.findMessageIndex(conversationId, messageId)

    fun findUnreadMessagesSync(conversationId: String) = messageDao.findUnreadMessagesSync(conversationId)
}
