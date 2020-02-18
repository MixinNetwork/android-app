package one.mixin.android.util.mention.syntax.node

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.CharacterStyle

open class StyleNode<RC, T>(val styles: List<T>) : Node<RC>() {

  override fun render(builder: SpannableStringBuilder, renderContext: RC) {
    val startIndex = builder.length

    // First render all child nodes, as these are the nodes we want to apply the styles to.
    getChildren()?.forEach { it.render(builder, renderContext) }

    styles.forEach { builder.setSpan(it, startIndex, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) }
  }

  override fun toString() = "${javaClass.simpleName} >\n" +
      getChildren()?.joinToString("\n->", prefix = ">>", postfix = "\n>|") {
        it.toString()
      }

  companion object {

    /**
     * Convenience method for creating a [StyleNode] when we already know what
     * the text content will be.
     */
    @JvmStatic
    fun <RC> createWithText(content: String, styles: List<CharacterStyle>): StyleNode<RC, CharacterStyle> {
      val styleNode = StyleNode<RC, CharacterStyle>(styles)
      styleNode.addChild(TextNode(content))
      return styleNode
    }
  }
}
