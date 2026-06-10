package one.mixin.android.util.markdown

import io.noties.prism4j.GrammarLocator
import io.noties.prism4j.Prism4j

class LanguageGrammerLocator : GrammarLocator {
    override fun grammar(
        prism4j: Prism4j,
        language: String,
    ): Prism4j.Grammar? =
        when (language) {
            "c" -> Prism_c.create(prism4j)
            "clike" -> Prism_clike.create(prism4j)
            "cpp" -> Prism_cpp.create(prism4j)
            "csharp" -> Prism_csharp.create(prism4j)
            "css" -> Prism_css.create(prism4j)
            "dart" -> Prism_dart.create(prism4j)
            "git" -> Prism_git.create(prism4j)
            "java" -> Prism_java.create(prism4j)
            "javascript" -> Prism_javascript.create(prism4j)
            "json" -> Prism_json.create(prism4j)
            "kotlin" -> Prism_kotlin.create(prism4j)
            "python" -> Prism_python.create(prism4j)
            "sql" -> Prism_sql.create(prism4j)
            "swift" -> Prism_swift.create(prism4j)
            else -> Prism_json.create(prism4j)
        }

    override fun languages(): MutableSet<String> =
        mutableSetOf("c", "clike", "cpp", "csharp", "css", "dart", "git", "java", "javascript", "json", "kotlin", "python", "sql", "swift")
}
