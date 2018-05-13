package one.mixin.android.ui.wallet

import android.arch.lifecycle.ViewModel
import one.mixin.android.repository.AccountRepository
import javax.inject.Inject

class PinCheckViewModel @Inject
internal constructor(private val accountRepository: AccountRepository) : ViewModel() {

    fun verifyPin(code: String) =
        accountRepository.verifyPin(code)
}