package one.mixin.android.util.mention.syntax.simple

import android.text.SpannableStringBuilder
import one.mixin.android.util.mention.syntax.node.Node
import one.mixin.android.util.mention.syntax.parser.Parser
import one.mixin.android.util.mention.syntax.parser.Rule

object SimpleRenderer {

  @JvmStatic
  fun <R> render(source: CharSequence, rules: Collection<Rule<R, Node<R>>>, renderContext: R): SpannableStringBuilder {
    val parser = Parser<R, Node<R>>()
    for (rule in rules) {
      parser.addRule(rule)
    }

    return render(SpannableStringBuilder(), parser.parse(source), renderContext)
  }

  @JvmStatic
  fun <R> render(source: CharSequence, parser: Parser<R, Node<R>>, renderContext: R): SpannableStringBuilder {
    return render(SpannableStringBuilder(), parser.parse(source), renderContext)
  }

  @JvmStatic
  fun <T : SpannableStringBuilder, R> render(builder: T, ast: Collection<Node<R>>, renderContext: R): T {
    for (node in ast) {
      node.render(builder, renderContext)
    }
    return builder
  }
}
