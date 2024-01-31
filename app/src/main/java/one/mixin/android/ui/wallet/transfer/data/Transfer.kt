package one.mixin.android.ui.wallet.transfer.data

import one.mixin.android.vo.Address
import one.mixin.android.vo.safe.TokenItem

data class Transfer(
    val type:TransferType,
    val status: TransferStatus,
    val asset:TokenItem,
    val amount:String,
    val address:Address?,
    val fee:List<TokenItem>?,
    val feeAmount:String?,
)