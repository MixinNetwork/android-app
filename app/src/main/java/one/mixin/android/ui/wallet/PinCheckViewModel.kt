package one.mixin.android.ui.wallet

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import one.mixin.android.repository.AccountRepository

class PinCheckViewModel @ViewModelInject
internal constructor(private val accountRepository: AccountRepository) : ViewModel() {

    suspend fun verifyPin(code: String) = accountRepository.verifyPin(code)

    suspend fun errorCount() = accountRepository.errorCount()
}
