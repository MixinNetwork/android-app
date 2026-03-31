package one.mixin.android.repository

import io.reactivex.Observable
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.AccountRequest
import one.mixin.android.api.request.AccountUpdateRequest
import one.mixin.android.api.request.DeactivateVerificationRequest
import one.mixin.android.api.request.VerificationRequest
import one.mixin.android.api.response.VerificationResponse
import one.mixin.android.api.service.AccountService
import one.mixin.android.vo.Account
import javax.inject.Inject

class LandingAccountRepository
    @Inject
    constructor(
        private val accountService: AccountService,
    ) {
        fun verificationObserver(request: VerificationRequest): Observable<MixinResponse<VerificationResponse>> =
            accountService.verificationObserver(request)

        suspend fun verification(request: VerificationRequest): MixinResponse<VerificationResponse> =
            accountService.verification(request)

        suspend fun create(
            id: String,
            request: AccountRequest,
        ): MixinResponse<Account> =
            accountService.create(id, request)

        suspend fun changePhone(
            id: String,
            request: AccountRequest,
        ): MixinResponse<Account> =
            accountService.changePhone(id, request)

        fun deactiveVerification(
            id: String,
            request: DeactivateVerificationRequest,
        ): Observable<MixinResponse<VerificationResponse>> =
            accountService.deactiveVerification(id, request)

        fun update(request: AccountUpdateRequest): Observable<MixinResponse<Account>> =
            accountService.update(request)
    }
