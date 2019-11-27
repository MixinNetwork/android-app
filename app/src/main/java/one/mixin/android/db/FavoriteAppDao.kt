package one.mixin.android.db

import androidx.room.Dao

@Dao
interface FavoriteAppDao {
    companion object {
        const val QUERY = """
            SELECT u.user_id, u.full_name, u.biography, u.identity_number, u.avatar_url, 
            u.phone, u.is_verified, u.mute_until, u.app_id, u.relationship, u.created_at
            FROM users u
            LEFT JOIN favorite_apps ON favorite_apps.app_id = u.app_id
            WHERE favorite_apps.user_id = ?
            ORDER BY favorite_apps.created_at ASC
        """
    }


}