package one.mixin.android.db.converter

import androidx.room.TypeConverter
import one.mixin.android.api.response.AppMetadata
import one.mixin.android.api.response.Approval
import one.mixin.android.api.response.Web3Fee
import one.mixin.android.api.response.Web3Transfer
import one.mixin.android.util.GsonHelper

class Web3TypeConverters {
    private val gson = GsonHelper.customGson

    @TypeConverter
    fun fromFee(fee: Web3Fee): String = gson.toJson(fee)

    @TypeConverter
    fun toFee(json: String): Web3Fee = gson.fromJson(json, Web3Fee::class.java)

    @TypeConverter
    fun fromTransfers(transfers: List<Web3Transfer>): String = gson.toJson(transfers)

    @TypeConverter
    fun toTransfers(json: String): List<Web3Transfer> = gson.fromJson(json, Array<Web3Transfer>::class.java).toList()

    @TypeConverter
    fun fromApprovals(approvals: List<Approval>): String = gson.toJson(approvals)

    @TypeConverter
    fun toApprovals(json: String): List<Approval> = gson.fromJson(json, Array<Approval>::class.java).toList()

    @TypeConverter
    fun fromAppMetadata(metadata: AppMetadata?): String? = metadata?.let { gson.toJson(it) }

    @TypeConverter
    fun toAppMetadata(json: String?): AppMetadata? = json?.let { gson.fromJson(it, AppMetadata::class.java) }
}