package one.mixin.android.db.web3

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import one.mixin.android.api.response.Web3Address
import one.mixin.android.db.BaseDao

@Dao
interface Web3AddressDao : BaseDao<Web3Address> {
}
