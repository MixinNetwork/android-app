package one.mixin.android.ui.common

import android.support.v4.app.FragmentManager
import one.mixin.android.R
import one.mixin.android.ui.contacts.ContactsActivity
import one.mixin.android.ui.home.ConversationListFragment
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.ui.search.SearchFragment
import one.mixin.android.ui.wallet.WalletActivity
import javax.inject.Inject

class NavigationController
@Inject
constructor(mainActivity: MainActivity) {
    private val containerId: Int = R.id.container
    private val fragmentManager: FragmentManager = mainActivity.supportFragmentManager
    private val context = mainActivity

    fun pushContacts() {
        ContactsActivity.show(context)
    }

    fun pushWallet() {
        WalletActivity.show(context)
    }

    fun navigateToMessage() {
        val conversationListFragment = ConversationListFragment.newInstance()
        fragmentManager.beginTransaction()
            .replace(containerId, conversationListFragment)
            .commitAllowingStateLoss()
    }

    fun showSearch() {
        val searchFragment = SearchFragment.getInstance()
        fragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
            .add(containerId, searchFragment)
            .commitAllowingStateLoss()
    }

    fun hideSearch() {
        fragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
            .remove(SearchFragment.getInstance())
            .commitAllowingStateLoss()
    }
}
