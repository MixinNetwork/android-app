package one.mixin.android.ui.common

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.session.Session
import one.mixin.android.ui.contacts.ContactsActivity
import one.mixin.android.ui.home.ConversationListFragment
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.ui.home.bot.BotManagerFragment
import one.mixin.android.ui.search.SearchFragment
import one.mixin.android.ui.tip.TipActivity
import one.mixin.android.ui.tip.TipBundle
import one.mixin.android.ui.tip.TipType
import one.mixin.android.ui.tip.TryConnecting
import one.mixin.android.ui.wallet.WalletActivity

class NavigationController
    constructor(mainActivity: MainActivity) {
        private val containerId: Int = R.id.container
        private val fragmentManager: FragmentManager = mainActivity.supportFragmentManager
        private val context = mainActivity

        fun pushContacts() {
            ContactsActivity.show(context)
        }

        fun pushWallet(deviceId: String? = null) {
            if (Session.getAccount()?.hasPin == true) {
                WalletActivity.show(context)
            } else {
                val id =
                    deviceId
                        ?: requireNotNull(context.defaultSharedPreferences.getString(Constants.DEVICE_ID, null)) { "required deviceId can not be null" }
                TipActivity.show(context, TipBundle(TipType.Create, id, TryConnecting))
            }
        }

        fun navigateToMessage(conversationListFragment: ConversationListFragment) {
            fragmentManager.beginTransaction()
                .replace(containerId, conversationListFragment, ConversationListFragment.TAG)
                .commitAllowingStateLoss()
        }

        fun navigateToBotManager(botManagerFragment: BotManagerFragment) {
            fragmentManager.beginTransaction()
                .replace(containerId, botManagerFragment, BotManagerFragment.TAG)
                .commitAllowingStateLoss()
        }


        fun showSearch() {
            var searchFragment = fragmentManager.findFragmentByTag(SearchFragment.TAG)
            if (searchFragment == null) {
                searchFragment = SearchFragment()
                fragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                    .add(containerId, searchFragment, SearchFragment.TAG)
                    .commitAllowingStateLoss()
            } else {
                searchFragment.view?.isVisible = true
                searchFragment.view?.animate()?.alpha(1f)?.start()
            }
        }

        fun hideSearch() {
            val f = fragmentManager.findFragmentByTag(SearchFragment.TAG)
            f?.view?.animate()?.apply {
                setListener(
                    object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            setListener(null)
                            f.view?.isVisible = false
                        }
                    },
                )
            }?.alpha(0f)?.start()
        }
    }
