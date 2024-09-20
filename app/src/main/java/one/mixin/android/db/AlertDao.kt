package one.mixin.android.db

import androidx.room.Dao
import one.mixin.android.ui.wallet.alert.vo.Alert

@Dao
interface AlertDao : BaseDao<Alert>