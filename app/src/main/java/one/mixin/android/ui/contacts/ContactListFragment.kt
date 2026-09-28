package one.mixin.android.ui.contacts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.showUserBottom

@AndroidEntryPoint
class ContactListFragment : BaseFragment() {
    companion object {
        const val TAG = "ContactListFragment"
    }

    private val viewModel by viewModels<ContactViewModel>()
    private val contacts by lazy { viewModel.findContacts() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val users by contacts.observeAsState(emptyList())
                ContactsPage(users, { requireActivity().onBackPressedDispatcher.onBackPressed() }, { showUserBottom(parentFragmentManager, it) })
            }
        }
}
