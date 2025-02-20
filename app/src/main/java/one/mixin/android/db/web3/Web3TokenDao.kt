package one.mixin.android.db.web3

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RoomWarnings
import one.mixin.android.api.response.Web3Token
import one.mixin.android.db.BaseDao
import one.mixin.android.db.TokenDao.Companion.POSTFIX_ASSET_ITEM_NOT_HIDDEN
import one.mixin.android.db.TokenDao.Companion.PREFIX_ASSET_ITEM
import one.mixin.android.ui.wallet.alert.vo.Alert
import one.mixin.android.vo.safe.TokenItem

@Dao
interface Web3TokenDao : BaseDao<Web3Token> {

    @Query("SELECT * FROM web3_tokens")
    fun web3Tokens(): LiveData<List<Web3Token>>
}
