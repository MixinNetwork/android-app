package one.mixin.android.widget

import java.net.URI

private val captchaDiagnosticUrl = Regex("(?:(?:https?|wss?)://|(?:file|data|blob):)[^\\s<>\"']+", RegexOption.IGNORE_CASE)
private val captchaDiagnosticSecret = Regex("(\\b(?:token|response|secret|key|sitekey|captchaId|captcha_id|captcha_output|pass_token|lot_number|challenge|authorization|cookie)\\b[\"']?\\s*[:=]\\s*)(?:\"[^\"]*\"|'[^']*'|[^\\s,;}&]+)", RegexOption.IGNORE_CASE)
private val captchaDiagnosticIdentifier = Regex("[A-Za-z0-9_+/=-]{32,}|[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}|(?<![A-Za-z0-9])\\+?\\d[\\d ()-]{6,}\\d(?![A-Za-z0-9])")

internal fun captchaDiagnosticOrigin(value: String?): String =
    runCatching {
        val uri = URI(value.orEmpty())
        if (uri.scheme in setOf("http", "https") && !uri.host.isNullOrBlank()) {
            "${uri.scheme}://${uri.host}"
        } else {
            "[local-or-invalid-url]"
        }
    }.getOrDefault("[local-or-invalid-url]")

internal fun captchaDiagnosticText(value: String?): String {
    var text = value.orEmpty().take(4096).replace(Regex("[\\r\\n\\t]"), " ")
    text = captchaDiagnosticUrl.replace(text) { captchaDiagnosticOrigin(it.value) }
    text = captchaDiagnosticSecret.replace(text) { "${it.groupValues[1]}[redacted]" }
    return captchaDiagnosticIdentifier.replace(text, "[redacted]").take(512)
}
