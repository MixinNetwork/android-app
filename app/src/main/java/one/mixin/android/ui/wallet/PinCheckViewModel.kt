package one.mixin.android.ui.wallet

import androidx.lifecycle.ViewModel
import one.mixin.android.repository.AccountRepository
import javax.inject.Inject

class PinCheckViewModel @Inject
internal constructor(private val accountRepository: AccountRepository) : ViewModel() {

    suspend fun verifyPin(code: String) = accountRepository.verifyPin(code)

    suspend fun errorCount() = accountRepository.errorCount()
}
