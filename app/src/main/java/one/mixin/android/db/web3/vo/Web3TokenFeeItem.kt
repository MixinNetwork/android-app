package one.mixin.android.db.web3.vo

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

@Parcelize
data class Web3TokenFeeItem(
    val token: Web3TokenItem,
    val amount: BigDecimal,
    val fee: BigDecimal
): Parcelable
