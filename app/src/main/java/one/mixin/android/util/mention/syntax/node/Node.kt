package one.mixin.android.util.mention.syntax.node

import android.text.SpannableStringBuilder

open class Node<R> {

  private var children: MutableCollection<Node<R>>? = null

  fun getChildren(): Collection<Node<R>>? = children

  fun hasChildren(): Boolean = children?.isNotEmpty() == true

  fun addChild(child: Node<R>) {
    children = (children ?: ArrayList()).apply {
      add(child)
    }
  }

  open fun render(builder: SpannableStringBuilder, renderContext: R) {}
}
