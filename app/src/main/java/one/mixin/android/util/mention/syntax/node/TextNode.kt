package one.mixin.android.util.mention.syntax.node

import android.text.SpannableStringBuilder

/**
 * Node representing simple text.
 */
open class TextNode<R> (val content: String) : Node<R>() {
  override fun render(builder: SpannableStringBuilder, renderContext: R) {
    builder.append(content)
  }

  override fun toString() = "${javaClass.simpleName}[${getChildren()?.size}]: $content"
}
