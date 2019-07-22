package one.mixin.android.ui.wallet

import androidx.lifecycle.ViewModel
import javax.inject.Inject
import one.mixin.android.repository.AccountRepository

class PinCheckViewModel @Inject
internal constructor(private val accountRepository: AccountRepository) : ViewModel() {

    suspend fun verifyPin(code: String) = accountRepository.verifyPin(code)
}
