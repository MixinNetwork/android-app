package one.mixin.android.db

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RoomWarnings
import androidx.room.Transaction
import one.mixin.android.vo.Participant
import one.mixin.android.vo.ParticipantItem
import one.mixin.android.vo.User

@Dao
interface ParticipantDao : BaseDao<Participant> {
    companion object {
        const val PREFIX_PARTICIPANT_ITEM = """
            SELECT p.conversation_id as conversationId, p.role as role, p.created_at as createdAt, 
            u.user_id as userId, u.identity_number as identityNumber, u.relationship as relationship, u.biography as biography, u.full_name as fullName, 
            u.avatar_url as avatarUrl, u.phone as phone, u.is_verified as isVerified, u.created_at as userCreatedAt, u.mute_until as muteUntil,
            u.has_pin as hasPin, u.app_id as appId, u.is_scam as isScam, u.membership as membership
        """
    }

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        """
        SELECT u.user_id, u.identity_number, u.full_name, u.avatar_url, u.relationship, u.biography FROM participants p, users u 
        WHERE p.conversation_id = :conversationId AND p.user_id = u.user_id
        """,
    )
    fun getParticipants(conversationId: String): List<User>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        """
        SELECT u.user_id, u.identity_number, u.full_name, u.avatar_url, u.relationship, u.biography 
        FROM participants p, users u 
        WHERE p.conversation_id = :conversationId 
        AND p.user_id = u.user_id
        AND u.app_id IS NULL
        AND u.relationship != 'ME'
        """,
    )
    suspend fun getParticipantsWithoutBot(conversationId: String): List<User>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        """
            $PREFIX_PARTICIPANT_ITEM
            FROM participants p, users u
            WHERE p.conversation_id = :conversationId 
            AND p.user_id = u.user_id 
            ORDER BY p.created_at DESC
        """,
    )
    fun observeGroupParticipants(conversationId: String): DataSource.Factory<Int, ParticipantItem>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        """
            $PREFIX_PARTICIPANT_ITEM
            FROM participants p, users u
            WHERE p.conversation_id = :conversationId 
            AND p.user_id = u.user_id
            AND (u.full_name LIKE '%' || :username || '%' ${BaseDao.ESCAPE_SUFFIX} OR u.identity_number like '%' || :identityNumber || '%' ${BaseDao.ESCAPE_SUFFIX})
            ORDER BY p.created_at DESC
        """,
    )
    fun fuzzySearchGroupParticipants(
        conversationId: String,
        username: String,
        identityNumber: String,
    ): DataSource.Factory<Int, ParticipantItem>

    @Query("UPDATE participants SET role = :role where conversation_id = :conversationId AND user_id = :userId")
    fun updateParticipantRole(
        conversationId: String,
        userId: String,
        role: String,
    )

    @Transaction
    @Query("SELECT * FROM participants WHERE conversation_id = :conversationId")
    fun getRealParticipants(conversationId: String): List<Participant>

    @Transaction
    fun replaceAll(
        conversationId: String,
        participants: List<Participant>,
    ) {
        deleteByConversationId(conversationId)
        insertList(participants)
    }

    @Query("DELETE FROM participants WHERE conversation_id = :conversationId")
    fun deleteByConversationId(conversationId: String)

    @Query("DELETE FROM participants WHERE conversation_id = :conversationId AND user_id = :userId")
    fun deleteById(
        conversationId: String,
        userId: String,
    )

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        "SELECT u.user_id, u.identity_number, u.biography, u.full_name, u.avatar_url, u.relationship FROM participants p, users u " +
            "WHERE p.conversation_id = :conversationId AND p.user_id = u.user_id ORDER BY p.created_at LIMIT 4",
    )
    fun getParticipantsAvatar(conversationId: String): List<User>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        "SELECT u.user_id, u.identity_number, u.biography, u.full_name, u.avatar_url, u.relationship FROM participants p, users u " +
            "WHERE p.conversation_id = :conversationId AND p.user_id = u.user_id ORDER BY p.created_at DESC LIMIT :limit",
    )
    fun getLimitParticipants(
        conversationId: String,
        limit: Int,
    ): List<User>

    @Query("SELECT * FROM participants WHERE conversation_id = :conversationId AND user_id = :userId")
    fun findParticipantById(
        conversationId: String,
        userId: String,
    ): Participant?

    @Query("SELECT count(1) FROM participants WHERE conversation_id = :conversationId")
    suspend fun getParticipantsCount(conversationId: String): Int

    @Query("SELECT count(1) FROM participants WHERE conversation_id = :conversationId")
    fun observeParticipantsCount(conversationId: String): LiveData<Int>

    @Query("SELECT * FROM participants")
    suspend fun getAllParticipants(): List<Participant>

    @Query(
        "SELECT p.conversation_id FROM participants p, conversations c WHERE p.user_id = :userId AND p.conversation_id = c.conversation_id AND c.status = 2 LIMIT 1",
    )
    fun joinedConversationId(userId: String): String?

    @Query("SELECT p.* FROM participants p WHERE p.rowid > :rowId ORDER BY p.rowid ASC LIMIT :limit")
    fun getParticipantsByLimitAndRowId(
        limit: Int,
        rowId: Long,
    ): List<Participant>

    @Query("SELECT p.* FROM participants p WHERE p.rowid > :rowId AND conversation_id IN (:conversationIds) ORDER BY p.rowid ASC LIMIT :limit")
    fun getParticipantsByLimitAndRowId(
        limit: Int,
        rowId: Long,
        conversationIds: Collection<String>,
    ): List<Participant>

    @Query("SELECT rowid FROM participants WHERE conversation_id = :conversationId AND user_id = :userId")
    fun getParticipantRowId(
        conversationId: String,
        userId: String,
    ): Long?

    @Query("SELECT count(1) FROM participants")
    fun countParticipants(): Long

    @Query("SELECT count(1) FROM participants WHERE conversation_id IN (:conversationIds)")
    fun countParticipants(conversationIds: Collection<String>): Long

    @Query("SELECT count(1) FROM participants  WHERE rowid > :rowId")
    fun countParticipants(rowId: Long): Long

    @Query("SELECT count(1) FROM participants WHERE rowid > :rowId AND conversation_id IN (:conversationIds)")
    fun countParticipants(
        rowId: Long,
        conversationIds: Collection<String>,
    ): Long
}
