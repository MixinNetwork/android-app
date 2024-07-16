package one.mixin.android.ui.wallet

import one.mixin.android.vo.User
import one.mixin.android.vo.safe.TokenItem

class FilterCriteria {
    var tokenItem: TokenItem? = null
    var user: User? = null
    var startTime: String? = null
    var endTime: String? = null
}
