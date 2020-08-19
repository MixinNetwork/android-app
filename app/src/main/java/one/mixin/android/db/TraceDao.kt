package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.Trace

@Dao
interface TraceDao : BaseDao<Trace> {

    @Query("SELECT * FROM traces WHERE trace_id = :traceId")
    suspend fun suspendFindTraceById(traceId: String): Trace?

    @Query("DELETE FROM traces WHERE trace_id = :traceId")
    suspend fun suspendDeleteById(traceId: String)

    @Query(
        """
        SELECT * FROM traces 
        WHERE (opponent_id = :opponentId OR (destination = :destination AND tag = :tag))
        AND amount = CAST(:amount AS REAL) AND asset_id = :assetId
        ORDER BY created_at DESC
        LIMIT 1
    """
    )
    suspend fun suspendFindTrace(opponentId: String?, destination: String?, tag: String?, amount: String, assetId: String): Trace?

    @Query(" DELETE FROM traces WHERE created_at <= date('now', '-1 day')")
    suspend fun delete1DayAgoRecords()
}
