package one.mixin.android.util.markdown

import android.text.method.LinkMovementMethod
import android.widget.TextView
import io.noties.markwon.Markwon
import io.noties.markwon.recycler.SimpleEntry
import one.mixin.android.R
import org.commonmark.node.Node

class DefaultEntry : SimpleEntry(
    R.layout.layout_markdown_item,
    R.id.text
) {
    override fun bindHolder(markwon: Markwon, holder: Holder, node: Node) {
        holder.itemView.findViewById<TextView>(R.id.text).movementMethod = LinkMovementMethod.getInstance()
        super.bindHolder(markwon, holder, node)
    }
}
