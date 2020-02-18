package one.mixin.android.util.mention.syntax.parser

import android.util.Log
import java.util.ArrayList
import java.util.Stack
import one.mixin.android.util.mention.syntax.node.Node

open class Parser<R, T : Node<R>> @JvmOverloads constructor(private val enableDebugging: Boolean = false) {

  private val rules = ArrayList<Rule<R, out T>>()

  fun <C : T> addRule(rule: Rule<R, C>): Parser<R, T> {
    rules.add(rule)
    return this
  }

  fun <C : T> addRules(rules: Collection<Rule<R, C>>): Parser<R, T> {
    for (rule in rules) {
      addRule(rule)
    }
    return this
  }

  /**
   * Transforms the [source] to a AST of [Node]s using the provided [rules].
   *
   * @param rules Ordered [List] of rules to use to convert the source to nodes.
   *    If not set, the parser will use its global list of [Parser.rules].
   *
   * @throws ParseException for certain specific error flows.
   */
  @JvmOverloads
  fun parse(source: CharSequence?, rules: List<Rule<R, out T>> = this.rules): MutableList<T> {
    val remainingParses = Stack<ParseSpec<R, out T>>()
    val topLevelNodes = ArrayList<T>()

    var lastCapture: String? = null

    if (source != null && source.isNotEmpty()) {
      remainingParses.add(ParseSpec(null, 0, source.length))
    }

    while (remainingParses.isNotEmpty()) {
      val builder = remainingParses.pop()

      if (builder.startIndex >= builder.endIndex) {
        break
      }

      val inspectionSource = source?.subSequence(builder.startIndex, builder.endIndex) ?: continue
      val offset = builder.startIndex

      var foundRule = false
      for (rule in rules) {
        val matcher = rule.match(inspectionSource, lastCapture)
        if (matcher != null) {
          logMatch(rule, inspectionSource)
          val matcherSourceEnd = matcher.end() + offset
          foundRule = true

          val newBuilder = rule.parse(matcher, this)
          val parent = builder.root

          newBuilder.root?.let {
            parent?.addChild(it) ?: topLevelNodes.add(it)
          }

          if (matcherSourceEnd != builder.endIndex) {
            remainingParses.push(ParseSpec.createNonterminal(parent, matcherSourceEnd, builder.endIndex))
          }

          if (!newBuilder.isTerminal) {
            newBuilder.applyOffset(offset)
            remainingParses.push(newBuilder)
          }

          try {
            lastCapture = matcher.group(0)
          } catch (throwable: Throwable) {
            throw ParseException(message = "matcher found no matches", source = source, cause = throwable)
          }

          break
        } else {
          logMiss(rule, inspectionSource)
        }
      }

      if (!foundRule) {
        throw ParseException("failed to find rule to match source", source)
      }
    }

    return topLevelNodes
  }

  private fun <R, T : Node<R>> logMatch(rule: Rule<R, T>, source: CharSequence) {
    if (enableDebugging) {
      Log.i(TAG, "MATCH: with rule with pattern: " + rule.matcher.pattern().toString() + " to source: " + source)
    }
  }

  private fun <R, T : Node<R>> logMiss(rule: Rule<R, T>, source: CharSequence) {
    if (enableDebugging) {
      Log.i(TAG, "MISS: with rule with pattern: " + rule.matcher.pattern().toString() + " to source: " + source)
    }
  }

  companion object {

    private const val TAG = "Parser"
  }

  class ParseException(message: String, source: CharSequence?, cause: Throwable? = null) :
    RuntimeException("Error while parsing: $message \n Source: $source", cause)
}
