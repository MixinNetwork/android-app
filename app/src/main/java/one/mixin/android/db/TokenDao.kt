package one.mixin.android.db

import androidx.room.Dao
import one.mixin.android.vo.Token

@Dao
interface TokenDao : BaseDao<Token>