package one.mixin.android.util.markdown;

import io.noties.prism4j.Prism4j;
import org.jetbrains.annotations.NotNull;

import static io.noties.prism4j.Prism4j.*;
import static java.util.regex.Pattern.MULTILINE;
import static java.util.regex.Pattern.compile;

@SuppressWarnings("unused")
public class Prism_git {

  @NotNull
  public static Prism4j.Grammar create(@NotNull Prism4j prism4j) {
    return grammar("git",
      token("comment", pattern(compile("^#.*", MULTILINE))),
      token("deleted", pattern(compile("^[-–].*", MULTILINE))),
      token("inserted", pattern(compile("^\\+.*", MULTILINE))),
      token("string", pattern(compile("(\"|')(?:\\\\.|(?!\\1)[^\\\\\\r\\n])*\\1", MULTILINE))),
      token("command", pattern(
        compile("^.*\\$ git .*$", MULTILINE),
        false,
        false,
        null,
        grammar("inside",
          token("parameter", pattern(compile("\\s--?\\w+", MULTILINE)))
        )
      )),
      token("coord", pattern(compile("^@@.*@@$", MULTILINE))),
      token("commit_sha1", pattern(compile("^commit \\w{40}$", MULTILINE)))
    );
  }
}