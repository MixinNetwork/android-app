package one.mixin.android.ui.conversation.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import one.mixin.android.R
import one.mixin.android.extension.notNullElse
import one.mixin.android.ui.conversation.holder.AppHolder
import one.mixin.android.vo.App

class AppAdapter(private val type: String, private val onAppClickListener: OnAppClickListener) :
    RecyclerView.Adapter<AppHolder>() {

    var appList: List<App>? = null
        set(value) {
            field = value?.filter {
                it.capabilites?.contains(type) == true
            }
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppHolder =
        AppHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_app, parent, false))

    override fun getItemCount(): Int {
        return notNullElse(appList, { it.size }, 0)
    }

    override fun onBindViewHolder(holder: AppHolder, position: Int) {
        appList?.let {
            val app = it[position]
            holder.bind(app, position, position == itemCount - 1)
            holder.itemView.setOnClickListener {
                onAppClickListener.onAppClick(app.homeUri, app.name)
            }
        }
    }

    interface OnAppClickListener {
        fun onAppClick(url: String, name: String)
    }
}
