package one.mixin.android.db.converter

import androidx.room3.ColumnTypeConverter
import one.mixin.android.api.response.AppMetadata
import one.mixin.android.api.response.Approval
import one.mixin.android.api.response.Web3Fee
import one.mixin.android.api.response.Web3Transfer
import one.mixin.android.util.GsonHelper

class Web3TypeConverters {
    private val gson = GsonHelper.customGson

    @ColumnTypeConverter
    fun fromFee(fee: Web3Fee): String = gson.toJson(fee)

    @ColumnTypeConverter
    fun toFee(json: String): Web3Fee = gson.fromJson(json, Web3Fee::class.java)

    @ColumnTypeConverter
    fun fromTransfers(transfers: List<Web3Transfer>): String = gson.toJson(transfers)

    @ColumnTypeConverter
    fun toTransfers(json: String): List<Web3Transfer> = gson.fromJson(json, Array<Web3Transfer>::class.java).toList()

    @ColumnTypeConverter
    fun fromApprovals(approvals: List<Approval>): String = gson.toJson(approvals)

    @ColumnTypeConverter
    fun toApprovals(json: String): List<Approval> = gson.fromJson(json, Array<Approval>::class.java).toList()

    @ColumnTypeConverter
    fun fromAppMetadata(metadata: AppMetadata?): String? = metadata?.let { gson.toJson(it) }

    @ColumnTypeConverter
    fun toAppMetadata(json: String?): AppMetadata? = json?.let { gson.fromJson(it, AppMetadata::class.java) }

}