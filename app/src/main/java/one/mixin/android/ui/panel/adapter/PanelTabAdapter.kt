package one.mixin.android.ui.panel.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.layout_sticker_tab.view.*
import one.mixin.android.R
import one.mixin.android.extension.loadCircleImage
import one.mixin.android.ui.panel.PanelTab
import one.mixin.android.ui.panel.PanelTabType
import one.mixin.android.vo.App

class PanelTabAdapter(
    private val isGroup: Boolean,
    private val isBot: Boolean,
    private val isSelfCreatedBot: Boolean
) : RecyclerView.Adapter<PanelTabAdapter.PanelHolder>() {

    private val buildInPanelTabs = arrayListOf<PanelTab>().apply {
        add(PanelTab(PanelTabType.Gallery, R.drawable.ic_panel_camera, null, true, true, true))
        val transferPanelTab = PanelTab(PanelTabType.Transfer, R.drawable.ic_panel_transfer, null, false, true, false)
        val voicePanelTab = PanelTab(PanelTabType.Voice, R.drawable.ic_panel_voice, null, true, false, false)
        if (isBot && isSelfCreatedBot) {
            add(transferPanelTab)
        } else if (!isGroup) {
            add(transferPanelTab)
            add(voicePanelTab)
        }
        add(PanelTab(PanelTabType.File, R.drawable.ic_panel_file, null, false, true, false))
        add(PanelTab(PanelTabType.Contact, R.drawable.ic_panel_contact, null, true, true, false))
    }

    var onPanelTabListener: OnPanelTabListener? = null

    var appList = listOf<App>()
        set(value) {
            if (field == value) return
            field = value

            panelTabs = mutableListOf<PanelTab>().apply {
                addAll(buildInPanelTabs)
            }
            for (app in appList) {
                panelTabs.add(PanelTab(PanelTabType.App, null, app.icon_url, true, true, false))
            }

            notifyDataSetChanged()
        }

    private var panelTabs = mutableListOf<PanelTab>().apply {
        addAll(buildInPanelTabs)
    }
    private var lastCheckedPanel: PanelTab = panelTabs[0]

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        PanelHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_panel_tab, parent, false))

    override fun getItemCount() = panelTabs.size

    override fun onBindViewHolder(holder: PanelHolder, position: Int) {
        val view = holder.itemView
        val panelTab = panelTabs[position]
        if (panelTab.icon != null) {
            view.icon.setImageResource(panelTab.icon)
        } else {
            view.icon.loadCircleImage(panelTab.iconUrl)
        }
        if (panelTab.checked) {
            view.setBackgroundResource(R.drawable.bg_sticker_tab)
        } else {
            view.setBackgroundResource(android.R.color.transparent)
        }
        view.setOnClickListener {
            if (panelTab.checked) return@setOnClickListener

            if (panelTab.checkable) {
                lastCheckedPanel.checked = false
                panelTab.checked = true
                lastCheckedPanel = panelTab
                notifyDataSetChanged()
            }
            onPanelTabListener?.onPanelTabClick(panelTab)
        }
    }

    class PanelHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    interface OnPanelTabListener {
        fun onPanelTabClick(panelTab: PanelTab)
    }
}