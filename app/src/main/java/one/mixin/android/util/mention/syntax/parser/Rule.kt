package one.mixin.android.util.mention.syntax.parser

import java.util.regex.Matcher
import java.util.regex.Pattern
import one.mixin.android.util.mention.syntax.node.Node

abstract class Rule<R, T : Node<R>>(val matcher: Matcher) {

  constructor(pattern: Pattern) : this(pattern.matcher(""))

  open fun match(inspectionSource: CharSequence, lastCapture: String?): Matcher? {
    matcher.reset(inspectionSource)
    return if (matcher.find()) matcher else null
  }

  abstract fun parse(matcher: Matcher, parser: Parser<R, in T>): ParseSpec<R, T>
}
