package one.mixin.android.api.response

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import one.mixin.android.db.converter.FiatOrderConverter

@Parcelize
@Entity(tableName = "member_orders")
@TypeConverters(FiatOrderConverter::class)
data class MemberOrder(
    @SerializedName("type")
    @ColumnInfo(name = "type")
    val type: String,
    @SerializedName("order_id")
    @PrimaryKey
    @ColumnInfo(name = "order_id")
    val orderId: String,
    @SerializedName("user_id")
    @ColumnInfo(name = "user_id")
    val userId: String,
    @SerializedName("category")
    @ColumnInfo(name = "category")
    val category: String,
    @SerializedName("asset_id")
    @ColumnInfo(name = "asset_id")
    val assetId: String,
    @SerializedName("amount_original")
    @ColumnInfo(name = "amount_original")
    val amountOriginal: String,
    @SerializedName("amount")
    @ColumnInfo(name = "amount")
    val amount: String,
    @SerializedName("amount_actual")
    @ColumnInfo(name = "amount_actual")
    val amountActual: String?,
    @SerializedName("asset_amount")
    @ColumnInfo(name = "asset_amount")
    val assetAmount: String?,
    @SerializedName("status")
    @ColumnInfo(name = "status")
    val status: String,
    @SerializedName("before")
    @ColumnInfo(name = "before")
    val before: String,
    @SerializedName("after")
    @ColumnInfo(name = "after")
    val after: String,
    @SerializedName("quantity_accounts")
    @ColumnInfo(name = "quantity_accounts")
    val quantityAccounts: Long,
    @SerializedName("quantity_transactions")
    @ColumnInfo(name = "quantity_transactions")
    val quantityTransactions: Long,
    @SerializedName("method")
    @ColumnInfo(name = "method")
    val method: String,
    @SerializedName("source")
    @ColumnInfo(name = "source")
    val source: String,
    @SerializedName("reason")
    @ColumnInfo(name = "reason")
    val reason: String,
    @SerializedName("stars")
    @ColumnInfo(name = "stars")
    val stars: Long,
    @SerializedName("paid_at")
    @ColumnInfo(name = "paid_at")
    val paidAt: String?,
    @SerializedName("expired_at")
    @ColumnInfo(name = "expired_at")
    val expiredAt: String?,
    @SerializedName("created_at")
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @SerializedName("payment_url")
    @ColumnInfo(name = "payment_url")
    val paymentUrl: String?,
    @SerializedName("fiat_order")
    @ColumnInfo(name = "fiat_order")
    val fiatOrder: FiatOrderView?,
    @SerializedName("checkout_user_id")
    @ColumnInfo(name = "checkout_user_id")
    val checkoutUserId: String?,
    @SerializedName("checkout_memo")
    @ColumnInfo(name = "checkout_memo")
    val checkoutMemo: String?,
    @SerializedName("play_store_subscription_id")
    @ColumnInfo(name = "play_store_subscription_id")
    val playStoreSubscriptionId: String? = null,
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
