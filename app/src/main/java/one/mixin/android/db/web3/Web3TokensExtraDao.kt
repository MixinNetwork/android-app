package one.mixin.android.db.web3

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.db.BaseDao
import one.mixin.android.db.web3.vo.Web3TokensExtra

/**
 * 用于操作 tokens_extra 表的 DAO 接口
 */
@Dao
interface Web3TokensExtraDao : BaseDao<Web3TokensExtra> {
    
    /**
     * 根据代币 ID 查询额外信息
     */
    @Query("SELECT * FROM tokens_extra WHERE coin_id = :coinId")
    suspend fun findByCoinId(coinId: String): Web3TokensExtra?
    
    /**
     * 获取所有隐藏的代币 ID
     */
    @Query("SELECT coin_id FROM tokens_extra WHERE hidden = 1")
    fun getHiddenCoinIds(): LiveData<List<String>>
    
    /**
     * 更新代币的隐藏状态
     */
    @Query("UPDATE tokens_extra SET hidden = :hidden, updated_at = :updatedAt WHERE coin_id = :coinId")
    suspend fun updateHidden(coinId: String, hidden: Boolean, updatedAt: String)
}
