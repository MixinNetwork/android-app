package one.mixin.android.util.mention.syntax.parser

import one.mixin.android.util.mention.syntax.node.Node

class ParseSpec<R, T : Node<R>> {
  val root: T?
  val isTerminal: Boolean
  var startIndex: Int = 0
  var endIndex: Int = 0

  constructor(root: T?, startIndex: Int, endIndex: Int) {
    this.root = root
    this.isTerminal = false
    this.startIndex = startIndex
    this.endIndex = endIndex
  }

  constructor(root: T?) {
    this.root = root
    this.isTerminal = true
  }

  fun applyOffset(offset: Int) {
    startIndex += offset
    endIndex += offset
  }

  companion object {

    @JvmStatic
    fun <R, T : Node<R>> createNonterminal(node: T?, startIndex: Int, endIndex: Int): ParseSpec<R, T> {
      return ParseSpec(node, startIndex, endIndex)
    }

    @JvmStatic
    fun <R, T : Node<R>> createTerminal(node: T?): ParseSpec<R, T> {
      return ParseSpec(node)
    }
  }
}
