package one.mixin.android.ui.panel.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_panel_file.view.*
import one.mixin.android.R
import one.mixin.android.extension.fileSize
import one.mixin.android.ui.common.recyclerview.HeaderAdapter
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

internal class PanelFileAdapter : HeaderAdapter<File>() {

    private val dateFormat = SimpleDateFormat("yyyy/MM/dd")

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is NormalHolder) {
            val v = holder.itemView
            val f = data!![getPos(position)]
            v.name_tv.text = f.name
            v.size_tv.text = f.length().fileSize()
            v.time_tv.text = dateFormat.format(Date(f.lastModified()))
            v.setOnClickListener {
                onItemListener?.onNormalItemClick(f)
            }
        }
    }

    override fun getNormalViewHolder(context: Context, parent: ViewGroup) =
        NormalHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_panel_file, parent, false))
}