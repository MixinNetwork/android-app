package one.mixin.android.api.response

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import one.mixin.android.db.converter.FiatOrderConverter

@Parcelize
@Entity(tableName = "membership_orders", indices = [Index(value = ["created_at"])])
@TypeConverters(FiatOrderConverter::class)
data class MembershipOrder(
    @SerializedName("order_id")
    @PrimaryKey
    @ColumnInfo(name = "order_id")
    val orderId: String,
    @SerializedName("category")
    @ColumnInfo(name = "category")
    val category: String,
    @SerializedName("amount")
    @ColumnInfo(name = "amount")
    val amount: String,
    @SerializedName("amount_actual")
    @ColumnInfo(name = "amount_actual")
    val amountActual: String,
    @SerializedName("amount_original")
    @ColumnInfo(name = "amount_original")
    val amountOriginal: String,
    @SerializedName("after")
    @ColumnInfo(name = "after")
    val after: String,
    @SerializedName("before")
    @ColumnInfo(name = "before")
    val before: String,
    @SerializedName("created_at")
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @SerializedName("fiat_order")
    @ColumnInfo(name = "fiat_order")
    val fiatOrder: FiatOrderView?,
    @SerializedName("stars")
    @ColumnInfo(name = "stars")
    val stars: Long,
    @SerializedName("payment_url")
    @ColumnInfo(name = "payment_url")
    val paymentUrl: String?,
    @SerializedName("status")
    @ColumnInfo(name = "status")
    val status: String
) : Parcelable

@Parcelize
data class FiatOrderView(
    @SerializedName("type")
    val type: String,
    @SerializedName("order_id")
    val orderId: String,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("source")
    val source: String,
    @SerializedName("subscription_id")
    val subscriptionId: String,
    @SerializedName("amount")
    val amount: String,
    @SerializedName("receipt")
    val receipt: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("paid_at")
    val paidAt: String,
    @SerializedName("created_at")
    val createdAt: String
) : Parcelable
