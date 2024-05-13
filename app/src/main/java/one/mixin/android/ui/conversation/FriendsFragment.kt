package one.mixin.android.ui.conversation

import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import one.mixin.android.R
import one.mixin.android.extension.withArgs
import one.mixin.android.job.MixinJobManager
import one.mixin.android.ui.common.friends.BaseFriendsFragment
import one.mixin.android.ui.common.friends.FriendsListener
import one.mixin.android.ui.conversation.adapter.FriendsAdapter
import one.mixin.android.ui.conversation.adapter.FriendsViewHolder
import one.mixin.android.vo.User

@AndroidEntryPoint
class FriendsFragment : BaseFriendsFragment<FriendsViewHolder>(), FriendsListener {
    init {
        adapter =
            FriendsAdapter(userCallback).apply {
                listener = this@FriendsFragment
            }
    }

    companion object {
        const val TAG = "FriendsFragment"
        const val ARGS_SEND = "args_send"

        fun newInstance(send: Boolean = false) = FriendsFragment().apply {
            withArgs {
                putBoolean(ARGS_SEND, send)
            }
        }
    }

    private val send by lazy {
        requireArguments().getBoolean(ARGS_SEND, false)
    }

    private val viewModel by viewModels<ConversationViewModel>()

    @Inject
    lateinit var jobManager: MixinJobManager

    override fun getTitleResId() = if (send)R.string.Send else R.string.Share_Contact

    override suspend fun getFriends() = if (send) viewModel.findFriendsNotBot() else viewModel.getFriends()

    private var friendClick: ((User) -> Unit)? = null

    fun setOnFriendClick(friendClick: (User) -> Unit) {
        this.friendClick = friendClick
    }

    override fun onBackPressed(): Boolean {
        if (send) {
            requireActivity().finish()
        } else {
            parentFragmentManager.popBackStackImmediate()
        }
        return true
    }

    override fun onItemClick(user: User) {
        if (friendClick != null) {
            friendClick?.invoke(user)
            try {
                parentFragmentManager.beginTransaction().remove(this).commit()
            } catch (ignored: IllegalStateException) {
            }
        } else {
        }
    }
}
