package one.mixin.android.ui.contacts

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.extension.dp
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.navTo
import one.mixin.android.extension.screenHeight
import one.mixin.android.ui.common.MixinComposeBottomSheetDialogFragment
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.group.GroupActivity

@AndroidEntryPoint
class NewChatBottomSheetDialogFragment : MixinComposeBottomSheetDialogFragment() {
    companion object {
        const val TAG = "NewChatBottomSheetDialogFragment"
    }

    private val viewModel by viewModels<ContactViewModel>()
    private val contacts by lazy { viewModel.findContacts() }

    @Composable
    override fun ComposeContent() {
        val users by contacts.observeAsState(emptyList())
        ContactsPage(
            contacts = users,
            onBack = { dismiss() },
            onContact = { user ->
                ConversationActivity.show(requireContext(), recipientId = user.userId)
                dismiss()
            },
            onNewGroup = {
                GroupActivity.show(requireContext())
                dismiss()
            },
            onAddContact = {
                navTo(AddPeopleFragment.newInstance(), AddPeopleFragment.TAG)
                dismiss()
            },
        )
    }

    override fun getBottomSheetHeight(view: View) = requireContext().screenHeight() - view.getSafeAreaInsetsTop() - 56.dp

    override fun showError(error: String) = Unit
}
