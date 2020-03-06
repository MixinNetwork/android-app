package one.mixin.android.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RoomWarnings
import one.mixin.android.db.BaseDao.Companion.ESCAPE_SUFFIX
import one.mixin.android.vo.App
import one.mixin.android.vo.AppItem

@Dao
interface AppDao : BaseDao<App> {
    companion object {
        const val PREFIX_APP_ITEM = """
            SELECT a.app_id as appId,a.app_number as appNumber, a.home_uri as homeUri, a.redirect_uri as redirectUri,
            a.name as name, a.icon_url as iconUrl, a.description as description, a.app_secret as appSecret,
            a.capabilities as capabilities, a.creator_id as creatorId, a.resource_patterns as resourcePatterns, 
            a.updated_at as updatedAt, u.user_id as userId, u.avatar_url as avatarUrl
        """
    }

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        """ 
            $PREFIX_APP_ITEM
            FROM apps a, participants p, users u WHERE p.conversation_id = :conversationId
            AND p.user_id = u.user_id AND a.app_id = u.app_id
            """
    )
    fun getGroupConversationApp(conversationId: String): LiveData<List<AppItem>>

    @Query(
        """
        $PREFIX_APP_ITEM
        FROM favorite_apps fa INNER JOIN apps a ON a.app_id = fa.app_id INNER JOIN users u ON u.user_id = fa.user_id
        WHERE fa.user_id in (:guestId, :masterId) 
        AND u.user_id IS NOT NULL ORDER BY CASE  WHEN fa.user_id= :guestId THEN 2 WHEN fa.user_id= :masterId THEN 1 END;
        """
    )
    fun getConversationApp(guestId: String, masterId: String): LiveData<List<AppItem>>

    @Query("SELECT * FROM apps WHERE app_id = :id")
    suspend fun findAppById(id: String): App?

    @Query("SELECT * FROM apps WHERE app_id IN(:appIds)")
    fun findAppsByIds(appIds: List<String>): List<App>

    @Query(" SELECT a.* FROM apps a WHERE a.home_uri LIKE :query $ESCAPE_SUFFIX")
    suspend fun searchAppByHost(query: String): List<App>

    @Query("SELECT a.* FROM apps a")
    suspend fun getApps(): List<App>

    @Query("SELECT a.* FROM favorite_apps fa INNER JOIN apps a ON fa.app_id = a.app_id WHERE fa.user_id =:userId ORDER BY fa.created_at ASC")
    suspend fun getFavoriteAppsByUserId(userId: String): List<App>

    @Query("""
        SELECT a.* FROM apps a INNER JOIN users u ON u.user_id = a.app_id WHERE u.relationship = 'FRIEND' AND a.app_id NOT IN (SELECT fa.app_id FROM favorite_apps fa WHERE fa.user_id == :userId)
    """)
    suspend fun getUnfavoriteApps(userId: String): List<App>
}
