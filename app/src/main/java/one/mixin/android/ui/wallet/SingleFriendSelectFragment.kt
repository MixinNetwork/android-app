package one.mixin.android.ui.wallet

import androidx.navigation.findNavController
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.friends.BaseFriendsFragment
import one.mixin.android.ui.common.friends.FriendsListener
import one.mixin.android.ui.conversation.ConversationViewModel
import one.mixin.android.ui.conversation.TransferFragment
import one.mixin.android.ui.setting.FriendsNoBotAdapter
import one.mixin.android.ui.setting.FriendsNoBotViewHolder
import one.mixin.android.util.Session
import one.mixin.android.vo.User

@AndroidEntryPoint
class SingleFriendSelectFragment : BaseFriendsFragment<FriendsNoBotViewHolder, ConversationViewModel>(), FriendsListener {
    init {
        adapter = FriendsNoBotAdapter(userCallback).apply {
            listener = this@SingleFriendSelectFragment
        }
    }

    override fun getTitleResId() = R.string.transfer_to

    override suspend fun getFriends() = viewModel.findFriendsNotBot()

    override fun getModelClass() = ConversationViewModel::class.java
    override fun onItemClick(user: User) {
        if (Session.getAccount()?.hasPin == true) {
            TransferFragment.newInstance(user.userId)
                .showNow(parentFragmentManager, TransferFragment.TAG)
            view?.findNavController()?.navigateUp()
        } else {
            toast(R.string.transfer_without_pin)
        }
    }
}
