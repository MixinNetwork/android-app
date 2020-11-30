package one.mixin.android.ui.conversation

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import one.mixin.android.R
import one.mixin.android.databinding.FragmentRecyclerViewBinding
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.adapter.Menu
import one.mixin.android.ui.conversation.adapter.MenuAdapter
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.AppItem

class MenuFragment : BaseFragment(R.layout.fragment_recycler_view) {
    companion object {
        const val TAG = "MenuFragment"

        const val ARGS_IS_GROUP = "is_group"
        const val ARGS_IS_BOT = "is_bot"
        const val ARGS_IS_SELF_CREATED_BOT = "is_self_created_bot"

        fun newInstance(
            isGroup: Boolean,
            isBot: Boolean,
            isSelfCreatedBot: Boolean
        ) = MenuFragment().withArgs {
            putBoolean(ARGS_IS_GROUP, isGroup)
            putBoolean(ARGS_IS_BOT, isBot)
            putBoolean(ARGS_IS_SELF_CREATED_BOT, isSelfCreatedBot)
        }
    }

    private val isGroup by lazy { requireArguments().getBoolean(ARGS_IS_GROUP) }
    private val isBot by lazy { requireArguments().getBoolean(ARGS_IS_BOT) }
    private val isSelfCreatedBot by lazy { requireArguments().getBoolean(ARGS_IS_SELF_CREATED_BOT) }

    private val menuAdapter by lazy { MenuAdapter(isGroup, isBot, isSelfCreatedBot) }

    private val binding by viewBinding(FragmentRecyclerViewBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        menuAdapter.onMenuListener = object : MenuAdapter.OnMenuListener {
            override fun onMenuClick(menu: Menu) {
                callback?.onMenuClick(menu)
            }
        }
        binding.apply {
            rv.layoutManager = GridLayoutManager(requireContext(), 4)
            rv.adapter = menuAdapter
        }
    }

    fun setAppList(appList: List<AppItem>) {
        menuAdapter.appList = appList
    }

    var callback: Callback? = null

    interface Callback {
        fun onMenuClick(menu: Menu)
    }
}
