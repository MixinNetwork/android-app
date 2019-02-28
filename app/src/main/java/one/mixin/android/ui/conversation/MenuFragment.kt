package one.mixin.android.ui.conversation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.android.synthetic.main.fragment_menu.*
import one.mixin.android.R
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.conversation.ConversationFragment.Companion.CONVERSATION_ID
import one.mixin.android.ui.conversation.adapter.Menu
import one.mixin.android.ui.conversation.adapter.MenuAdapter
import one.mixin.android.vo.App

class MenuFragment: Fragment() {
    companion object {
        const val TAG = "MenuFragment"

        const val ARGS_IS_GROUP = "is_group"
        const val ARGS_IS_BOT = "is_bot"
        const val ARGS_IS_SELF_CREATED_BOT = "is_self_created_bot"

        fun newInstance(
            isGroup: Boolean,
            isBot: Boolean,
            isSelfCreatedBot: Boolean,
            conversationId: String
        ) = MenuFragment().withArgs {
            putBoolean(ARGS_IS_GROUP, isGroup)
            putBoolean(ARGS_IS_BOT, isBot)
            putBoolean(ARGS_IS_SELF_CREATED_BOT, isSelfCreatedBot)
            putString(CONVERSATION_ID, conversationId)
        }
    }

    private val isGroup by lazy { arguments!!.getBoolean(ARGS_IS_GROUP) }
    private val isBot by lazy { arguments!!.getBoolean(ARGS_IS_BOT) }
    private val isSelfCreatedBot by lazy { arguments!!.getBoolean(ARGS_IS_SELF_CREATED_BOT) }
    private val conversationId by lazy { arguments!!.getString(CONVERSATION_ID) }

    private val menuAdapter by lazy { MenuAdapter(isGroup, isBot, isSelfCreatedBot) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_menu, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        menuAdapter.onMenuListener = object : MenuAdapter.OnMenuListener {
            override fun onMenuClick(menu: Menu) {
                callback?.onMenuClick(menu)
            }
        }
        menu_rv.layoutManager = GridLayoutManager(requireContext(), 4)
        menu_rv.adapter = menuAdapter
    }

    fun setAppList(appList: List<App>) {
        menuAdapter.appList = appList
    }

    var callback: Callback? = null

    interface Callback {
        fun onMenuClick(menu: Menu)
    }
}