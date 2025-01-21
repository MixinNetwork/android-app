package one.mixin.android.ui.wallet.transfer

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.repository.TokenRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.ui.wallet.transfer.data.TransferStatus
import one.mixin.android.vo.User
import one.mixin.android.vo.safe.TokenItem
import javax.inject.Inject

@HiltViewModel
class TransferViewModel
@Inject
internal constructor(
    val userRepository: UserRepository,
    val tokenRepository: TokenRepository,
) : ViewModel() {
    private val _status = MutableStateFlow(TransferStatus.AWAITING_CONFIRMATION)
    val status = _status.asStateFlow()
    var errorMessage: String? = null

    fun updateStatus(status: TransferStatus) {
        _status.value = status
    }

    suspend fun findLastWithdrawalSnapshotByReceiver(formatDestination: String) = tokenRepository.findLastWithdrawalSnapshotByReceiver(formatDestination)

    suspend fun findTokenItems(ids: List<String>): List<TokenItem> = tokenRepository.findTokenItems(ids)


    suspend fun findMultiUsers(
        userIds: List<String>,
    ):  List<User>? =
        withContext(Dispatchers.IO) {
            val existUserIds = userRepository.findUserExist(userIds)
            val queryUsers =
                userIds.filter {
                    !existUserIds.contains(it)
                }
            val users =
                if (queryUsers.isNotEmpty()) {
                    handleMixinResponse(
                        invokeNetwork = {
                            userRepository.fetchUser(queryUsers)
                        },
                        successBlock = {
                            val userList = it.data
                            if (userList != null) {
                                userRepository.upsertList(userList)
                            }
                            return@handleMixinResponse userRepository.findMultiUsersByIds(userIds.toSet())
                        },
                    ) ?: emptyList()
                } else {
                    userRepository.findMultiUsersByIds(userIds.toSet())
                }

            return@withContext users
        }

}
