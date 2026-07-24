package one.mixin.android.db.web3.vo

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Ignore
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.google.gson.annotations.SerializedName
import one.mixin.android.api.response.web3.ParsedTx

@Entity(
    tableName = "raw_transactions",
    indices = [Index(value = arrayOf("chain_id"))],
)
data class Web3RawTransaction(
    @PrimaryKey
    @ColumnInfo(name = "hash") 
    val hash: String,

    @ColumnInfo(name = "chain_id")
    @SerializedName("chain_id")
    val chainId: String,

    @ColumnInfo(name = "account")
    val account: String,

    @ColumnInfo(name = "nonce")
    val nonce: String,

    @ColumnInfo(name = "raw")
    val raw: String,

    @ColumnInfo(name = "state")
    val state: String,

    @ColumnInfo(name = "created_at")
    @SerializedName("created_at")
    val createdAt: String,

    @ColumnInfo(name = "updated_at")
    @SerializedName("updated_at")
    var updatedAt: String,
) {
    @Ignore
    @SerializedName("simulate_tx")
    var simulateTx: ParsedTx? = null
}

private const val GASLESS_PENDING_SPONSOR_PREFIX = "gasless:sponsor:"
private const val GASLESS_PENDING_BROADCAST_PREFIX = "gasless:broadcast:"

fun Web3RawTransaction.isGaslessPending(): Boolean =
    raw.startsWith(GASLESS_PENDING_SPONSOR_PREFIX) || raw.startsWith(GASLESS_PENDING_BROADCAST_PREFIX)

fun Web3RawTransaction.isGaslessSponsorPending(): Boolean =
    raw.startsWith(GASLESS_PENDING_SPONSOR_PREFIX)

fun buildGaslessSponsorPendingRawMarker(sponsorTxId: String): String =
    "$GASLESS_PENDING_SPONSOR_PREFIX$sponsorTxId"

fun buildGaslessBroadcastPendingRawMarker(broadcastTxHash: String): String =
    "$GASLESS_PENDING_BROADCAST_PREFIX$broadcastTxHash"
