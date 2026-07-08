package one.mixin.android.repository

import one.mixin.android.Constants.MIXIN_CASH_USER_ID
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.response.CashAccount
import one.mixin.android.api.service.CashService
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.util.ErrorHandler
import javax.inject.Inject

class CashRepository
    @Inject
    constructor(
        private val cashService: CashService,
        private val userRepository: UserRepository,
    ) {
        suspend fun cachedAccount(): CashAccount? =
            PropertyHelper.findCashAccount()

        suspend fun account(): MixinResponse<CashAccount> {
            userRepository.getBotPublicKey(MIXIN_CASH_USER_ID, false)
            val response = cashService.account()
            if (response.errorCode != ErrorHandler.AUTHENTICATION) {
                if (response.isSuccess) PropertyHelper.updateCashAccount(response.data)
                return response
            }

            userRepository.getBotPublicKey(MIXIN_CASH_USER_ID, true)
            return cashService.account().also {
                if (it.isSuccess) PropertyHelper.updateCashAccount(it.data)
            }
        }
    }
