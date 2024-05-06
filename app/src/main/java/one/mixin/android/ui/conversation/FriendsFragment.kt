package one.mixin.android.ui.conversation

import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.job.MixinJobManager
import one.mixin.android.ui.common.friends.BaseFriendsFragment
import one.mixin.android.ui.common.friends.FriendsListener
import one.mixin.android.ui.conversation.ConversationFragment.Companion.CONVERSATION_ID
import one.mixin.android.ui.conversation.adapter.FriendsAdapter
import one.mixin.android.ui.conversation.adapter.FriendsViewHolder
import one.mixin.android.ui.forward.ForwardActivity
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.ForwardAction
import one.mixin.android.vo.ForwardMessage
import one.mixin.android.vo.ShareCategory
import one.mixin.android.vo.User
import one.mixin.android.websocket.ContactMessagePayload
import javax.inject.Inject

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

        fun newInstance() = FriendsFragment()
    }

    private val viewModel by viewModels<ConversationViewModel>()

    @Inject
    lateinit var jobManager: MixinJobManager

    override fun getTitleResId() = R.string.Share_Contact

    override suspend fun getFriends() = viewModel.getFriends()

    private var friendClick: ((User) -> Unit)? = null

    fun setOnFriendClick(friendClick: (User) -> Unit) {
        this.friendClick = friendClick
    }

    override fun onBackPressed(): Boolean {
        parentFragmentManager.popBackStackImmediate()
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
