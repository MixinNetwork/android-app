package one.mixin.android.ui.conversation.link.parser

class ParserError(val code: Int, val symbol: String? = null, override val message: String? = null) : Exception()