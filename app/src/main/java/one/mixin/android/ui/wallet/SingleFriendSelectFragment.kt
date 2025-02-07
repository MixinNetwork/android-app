package one.mixin.android.ui.wallet

import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.toast
import one.mixin.android.session.Session
import one.mixin.android.ui.common.biometric.buildTransferBiometricItem
import one.mixin.android.ui.common.friends.BaseFriendsFragment
import one.mixin.android.ui.common.friends.FriendsListener
import one.mixin.android.ui.conversation.ConversationViewModel
import one.mixin.android.ui.conversation.TransferFragment
import one.mixin.android.ui.conversation.adapter.FriendsAdapter
import one.mixin.android.ui.conversation.adapter.FriendsViewHolder
import one.mixin.android.vo.User
import one.mixin.android.vo.safe.TokenItem

@AndroidEntryPoint
class SingleFriendSelectFragment : BaseFriendsFragment<FriendsViewHolder>(), FriendsListener {
    init {
        adapter =
            FriendsAdapter(userCallback).apply {
                listener = this@SingleFriendSelectFragment
            }
    }

    override fun getTitleResId() = R.string.Send_to_Contact

    override suspend fun getFriends() = viewModel.findFriendsAndMyBot()

    private val viewModel by viewModels<ConversationViewModel>()

    override fun onItemClick(user: User) {
        if (Session.getAccount()?.hasPin == true) {
            val token = requireArguments().getParcelableCompat(TransactionsFragment.ARGS_ASSET, TokenItem::class.java)!!
            TransferFragment.newInstance(buildTransferBiometricItem(user, token, "", null, null, null))
                .showNow(parentFragmentManager, TransferFragment.TAG)
            runCatching { view?.findNavController()?.navigateUp() }.onFailure {
                parentFragmentManager.beginTransaction().remove(this).commit()
            }
        } else {
            toast(R.string.transfer_without_pin)
        }
    }
}
