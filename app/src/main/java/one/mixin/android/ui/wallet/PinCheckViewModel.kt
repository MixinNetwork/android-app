package one.mixin.android.ui.wallet

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import one.mixin.android.repository.AccountRepository

@HiltViewModel
class PinCheckViewModel
    @Inject
    internal constructor(private val accountRepository: AccountRepository) : ViewModel() {
        suspend fun verifyPin(code: String) = accountRepository.verifyPin(code)

        suspend fun errorCount() = accountRepository.errorCount()
    }
