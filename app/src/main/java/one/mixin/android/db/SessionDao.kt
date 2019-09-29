package one.mixin.android.db

import androidx.room.Dao
import one.mixin.android.vo.Session

@Dao
interface SessionDao : BaseDao<Session>
