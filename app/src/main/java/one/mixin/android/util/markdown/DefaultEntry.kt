package one.mixin.android.util.markdown

import android.text.method.LinkMovementMethod
import io.noties.markwon.Markwon
import io.noties.markwon.recycler.SimpleEntry
import kotlinx.android.synthetic.main.layout_markdown_item.view.*
import one.mixin.android.R
import org.commonmark.node.Node

class DefaultEntry : SimpleEntry(
    R.layout.layout_markdown_item,
    R.id.text
) {
    override fun bindHolder(markwon: Markwon, holder: Holder, node: Node) {
        holder.itemView.text.movementMethod = LinkMovementMethod.getInstance()
        super.bindHolder(markwon, holder, node)
    }
}
