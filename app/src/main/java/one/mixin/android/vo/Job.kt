package one.mixin.android.vo

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "jobs")
class Job(
    @PrimaryKey
    @SerializedName("job_id")
    @ColumnInfo(name = "job_id")
    var jobId: String,

    @SerializedName("job_action")
    @ColumnInfo(name = "job_action")
    var action: String,

    @SerializedName("status")
    @ColumnInfo(name = "status")
    var status: String)