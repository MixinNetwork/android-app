package one.mixin.android.ui.panel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_panel_tab.*
import one.mixin.android.R
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.panel.adapter.PanelTabAdapter
import one.mixin.android.vo.App

class PanelTabFragment : BaseFragment() {
    companion object {
        const val TAG = "PanelTabFragment"

        const val ARGS_IS_GROUP = "is_group"
        const val ARGS_IS_BOT = "is_bot"
        const val ARGS_IS_SELF_CREATED_BOT = "is_self_created_bot"

        fun newInstance(
            isGroup: Boolean,
            isBot: Boolean,
            isSelfCreatedBot: Boolean
        ) = PanelTabFragment().withArgs {
            putBoolean(ARGS_IS_GROUP, isGroup)
            putBoolean(ARGS_IS_BOT, isBot)
            putBoolean(ARGS_IS_SELF_CREATED_BOT, isSelfCreatedBot)
        }
    }

    private val isGroup by lazy { arguments!!.getBoolean(ARGS_IS_GROUP) }
    private val isBot by lazy { arguments!!.getBoolean(ARGS_IS_BOT) }
    private val isSelfCreatedBot by lazy { arguments!!.getBoolean(ARGS_IS_SELF_CREATED_BOT) }

    private val panelTabAdapter by lazy { PanelTabAdapter(isGroup, isBot, isSelfCreatedBot) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_panel_tab, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        panel_tab_rv.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        panel_tab_rv.adapter = panelTabAdapter
        callback?.toggleExpand(panelTabAdapter.buildInPanelTabs[0])
        panelTabAdapter.onPanelTabListener = object : PanelTabAdapter.OnPanelTabListener {
            override fun onPanelTabClick(panelTab: PanelTab) {
                when (panelTab.type) {
                    PanelTabType.Gallery -> callback?.showGalleryFragment(panelTab)
                    PanelTabType.Transfer -> callback?.showTransferFragment(panelTab)
                    PanelTabType.Voice -> callback?.showVoiceFragment(panelTab)
                    PanelTabType.File -> callback?.showFileFragment(panelTab)
                    PanelTabType.Contact -> callback?.showContactFragment(panelTab)
                    PanelTabType.App -> callback?.showAppFragment(panelTab)
                }
                callback?.toggleExpand(panelTab)
            }
        }
    }

    fun setAppList(appList: List<App>) {
        panelTabAdapter.appList = appList
    }

    var callback: Callback? = null

    interface Callback {
        fun toggleExpand(panelTab: PanelTab)
        fun showGalleryFragment(panelTab: PanelTab)
        fun showTransferFragment(panelTab: PanelTab)
        fun showVoiceFragment(panelTab: PanelTab)
        fun showFileFragment(panelTab: PanelTab)
        fun showContactFragment(panelTab: PanelTab)
        fun showAppFragment(panelTab: PanelTab)
    }
}