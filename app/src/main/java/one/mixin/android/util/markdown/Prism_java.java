package one.mixin.android.util.markdown;

import io.noties.prism4j.GrammarUtils;
import io.noties.prism4j.Prism4j;
import io.noties.prism4j.annotations.Extend;
import org.jetbrains.annotations.NotNull;

import static io.noties.prism4j.Prism4j.*;
import static java.util.regex.Pattern.*;

@SuppressWarnings("unused")
@Extend("clike")
public class Prism_java {

  @NotNull
  public static Prism4j.Grammar create(@NotNull Prism4j prism4j) {

    final Prism4j.Token keyword = token("keyword", pattern(compile("\\b(?:abstract|continue|for|new|switch|assert|default|goto|package|synchronized|boolean|do|if|private|this|break|double|implements|protected|throw|byte|else|import|public|throws|case|enum|instanceof|return|transient|catch|extends|int|short|try|char|final|interface|static|void|class|finally|long|strictfp|volatile|const|float|native|super|while)\\b")));

    final Prism4j.Grammar java = GrammarUtils.extend(GrammarUtils.require(prism4j, "clike"), "java",
      keyword,
      token("number", pattern(compile("\\b0b[01]+\\b|\\b0x[\\da-f]*\\.?[\\da-fp-]+\\b|(?:\\b\\d+\\.?\\d*|\\B\\.\\d+)(?:e[+-]?\\d+)?[df]?", CASE_INSENSITIVE))),
      token("operator", pattern(
        compile("(^|[^.])(?:\\+[+=]?|-[-=]?|!=?|<<?=?|>>?>?=?|==?|&[&=]?|\\|[|=]?|\\*=?|\\/=?|%=?|\\^=?|[?:~])", MULTILINE),
        true
      ))
    );

    GrammarUtils.insertBeforeToken(java, "function",
      token("annotation", pattern(
        compile("(^|[^.])@\\w+"),
        true,
        false,
        "punctuation"
      ))
    );

    GrammarUtils.insertBeforeToken(java, "class-name",
      token("generics", pattern(
        compile("<\\s*\\w+(?:\\.\\w+)?(?:\\s*,\\s*\\w+(?:\\.\\w+)?)*>", CASE_INSENSITIVE),
        false,
        false,
        "function",
        grammar(
          "inside",
          keyword,
          token("punctuation", pattern(compile("[<>(),.:]")))
        )
      ))
    );

    return java;
  }
}