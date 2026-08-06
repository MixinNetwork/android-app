package one.mixin.android

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.Locale
import one.mixin.android.widget.picker.toTimeInterval
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.xmlpull.v1.XmlPullParser

@RunWith(AndroidJUnit4::class)
class StringResourceFormatInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun allLocalizedFormatArgumentsMatchAndRender() {
        val defaultResources = resourcesFor(Locale.ENGLISH)
        val stringIds = stringResourceIds()
        val issues = mutableListOf<String>()

        supportedLocales().forEach { locale ->
            val resources = resourcesFor(locale)
            stringIds.forEach { (name, id) ->
                val expected = analyze(defaultResources.getString(id))
                val localizedValue = resources.getString(id)
                val actual = analyze(localizedValue)

                if (actual.signature() != expected.signature()) {
                    issues +=
                        "$locale $name has ${actual.signature()}, expected ${expected.signature()}"
                    return@forEach
                }
                if (actual.placeholders.isEmpty()) {
                    if (actual.escapedPercentCount > 0) {
                        issues += "$locale $name contains %% but has no format argument"
                    }
                    if (IOS_PLACEHOLDER.containsMatchIn(localizedValue)) {
                        issues += "$locale $name contains an iOS placeholder"
                    }
                    return@forEach
                }
                if (actual.rawPercentCount > 0) {
                    issues += "$locale $name mixes format arguments with an unescaped %"
                    return@forEach
                }

                runCatching {
                    resources.getString(id, *actual.arguments())
                }.onSuccess { rendered ->
                    if (PLACEHOLDER.containsMatchIn(rendered) || rendered.contains("%%")) {
                        issues += "$locale $name did not fully render: $rendered"
                    }
                }.onFailure { error ->
                    issues += "$locale $name failed to render: ${error.message}"
                }
            }
        }

        assertTrue(issues.joinToString(prefix = "\n", separator = "\n"), issues.isEmpty())
    }

    @Test
    fun allLocalizedPluralFormatArgumentsMatchAndRender() {
        val defaultResources = resourcesFor(Locale.ENGLISH)
        val pluralIds = pluralResourceIds()
        val issues = mutableListOf<String>()

        supportedLocales().forEach { locale ->
            val resources = resourcesFor(locale)
            pluralIds.forEach { (name, id) ->
                PLURAL_QUANTITIES.forEach { quantity ->
                    val expected = analyze(defaultResources.getQuantityString(id, quantity))
                    val localizedValue = resources.getQuantityString(id, quantity)
                    val actual = analyze(localizedValue)

                    if (actual.signature() != expected.signature()) {
                        issues +=
                            "$locale $name[$quantity] has ${actual.signature()}, expected ${expected.signature()}"
                        return@forEach
                    }
                    if (actual.placeholders.isEmpty()) {
                        if (actual.escapedPercentCount > 0) {
                            issues += "$locale $name[$quantity] contains %% but has no format argument"
                        }
                        if (IOS_PLACEHOLDER.containsMatchIn(localizedValue)) {
                            issues += "$locale $name[$quantity] contains an iOS placeholder"
                        }
                        return@forEach
                    }
                    if (actual.rawPercentCount > 0) {
                        issues += "$locale $name[$quantity] mixes format arguments with an unescaped %"
                        return@forEach
                    }

                    runCatching {
                        resources.getQuantityString(id, quantity, *actual.arguments())
                    }.onSuccess { rendered ->
                        if (PLACEHOLDER.containsMatchIn(rendered) || rendered.contains("%%")) {
                            issues += "$locale $name[$quantity] did not fully render: $rendered"
                        }
                    }.onFailure { error ->
                        issues += "$locale $name[$quantity] failed to render: ${error.message}"
                    }
                }
            }
        }

        assertTrue(issues.joinToString(prefix = "\n", separator = "\n"), issues.isEmpty())
    }

    @Test
    fun russianTimeIntervalsUseActualQuantity() {
        val resources = resourcesFor(Locale.forLanguageTag("ru"))

        assertEquals("1 секунду", toTimeInterval(resources, 1L))
        assertEquals("2 секунды", toTimeInterval(resources, 2L))
        assertEquals("5 секунд", toTimeInterval(resources, 5L))
        assertEquals("21 секунду", toTimeInterval(resources, 21L))
    }

    @Test
    fun literalPercentRemainsSinglePercent() {
        supportedLocales().forEach { locale ->
            val resources = resourcesFor(locale)
            val rendered = resources.getString(R.string.slippage_invalid)

            assertEquals("$locale: $rendered", 2, rendered.count { it == '%' })
            assertFalse("$locale: $rendered", rendered.contains("%%"))
        }
    }

    @Test
    fun escapedPercentRendersAsSinglePercentWithArguments() {
        supportedLocales().forEach { locale ->
            val resources = resourcesFor(locale)
            val rendered = resources.getString(R.string.Exchanging_data, "42")

            assertTrue("$locale: $rendered", rendered.contains("42%"))
            assertFalse("$locale: $rendered", rendered.contains("%%"))
        }
    }

    @Test
    fun nonTranslatableNamesStayCanonical() {
        val names =
            mapOf(
                R.string.app_name to "Mixin",
                R.string.Mixin to "Mixin",
                R.string.member_title_mixin_safe to "Mixin Safe",
                R.string.member_title_mixin_star to "Mixin Star",
                R.string.Bitcoin to "Bitcoin",
                R.string.Ethereum to "Ethereum",
                R.string.Polygon to "Polygon",
                R.string.BSC to "BSC",
                R.string.Solana to "Solana",
                R.string.Base to "Base",
                R.string.TRON to "TRON",
                R.string.Optimism to "Optimism",
                R.string.Arbitrum to "Arbitrum",
                R.string.Avalanche to "Avalanche",
                R.string.HyperEVM to "HyperEVM",
                R.string.Toncoin to "TON",
            )

        supportedLocales().forEach { locale ->
            val resources = resourcesFor(locale)
            names.forEach { (id, expected) ->
                assertEquals("$locale: $expected", expected, resources.getString(id))
            }
        }
    }

    private fun stringResourceIds(): List<Pair<String, Int>> =
        R.string::class.java.fields
            .map { field -> field.name to field.getInt(null) }
            .sortedBy { it.first }

    private fun pluralResourceIds(): List<Pair<String, Int>> =
        R.plurals::class.java.fields
            .map { field -> field.name to field.getInt(null) }
            .sortedBy { it.first }

    private fun supportedLocales(): List<Locale> {
        val parser = context.resources.getXml(R.xml.locales_config)
        return buildList {
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG && parser.name == "locale") {
                    val languageTag = parser.getAttributeValue(ANDROID_NAMESPACE, "name")
                    add(Locale.forLanguageTag(languageTag))
                }
                parser.next()
            }
        }
    }

    private fun resourcesFor(locale: Locale): Resources {
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        return context.createConfigurationContext(configuration).resources
    }

    private fun analyze(value: String): FormatAnalysis {
        val placeholders = mutableListOf<Placeholder>()
        var escapedPercentCount = 0
        var rawPercentCount = 0
        var index = 0

        while (index < value.length) {
            if (value[index] != '%') {
                index++
                continue
            }
            if (value.startsWith("%%", index)) {
                escapedPercentCount++
                index += 2
                continue
            }
            val match = PLACEHOLDER.matchAt(value, index)
            if (match != null) {
                placeholders +=
                    Placeholder(
                        token = match.value,
                        explicitIndex = match.groups[1]?.value?.toInt(),
                        type = match.groups[2]!!.value.lowercase(),
                    )
                index = match.range.last + 1
                continue
            }
            rawPercentCount++
            index++
        }

        return FormatAnalysis(placeholders, escapedPercentCount, rawPercentCount)
    }

    private data class Placeholder(
        val token: String,
        val explicitIndex: Int?,
        val type: String,
    )

    private data class FormatAnalysis(
        val placeholders: List<Placeholder>,
        val escapedPercentCount: Int,
        val rawPercentCount: Int,
    ) {
        fun signature(): List<String> {
            var implicitIndex = 0
            return placeholders
                .map { placeholder ->
                    val argumentIndex = placeholder.explicitIndex ?: ++implicitIndex
                    "$argumentIndex:${placeholder.type}"
                }
                .sorted()
        }

        fun arguments(): Array<Any> {
            var implicitIndex = 0
            val types =
                placeholders.associate { placeholder ->
                    val argumentIndex = placeholder.explicitIndex ?: ++implicitIndex
                    argumentIndex to placeholder.type
                }
            val argumentCount = types.keys.maxOrNull() ?: 0
            return Array(argumentCount) { index ->
                when (types[index + 1]) {
                    "d" -> 42
                    "f" -> 42.5
                    else -> "ARG${index + 1}"
                }
            }
        }
    }

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
        val PLURAL_QUANTITIES = listOf(0, 1, 2, 3, 5, 11, 21, 22, 25, 101)
        val PLACEHOLDER = Regex("""%(?:(\d+)\$)?(?:\.\d+)?([sdf])""", RegexOption.IGNORE_CASE)
        val IOS_PLACEHOLDER = Regex("""%(?:\d+\$)?@""")
    }
}
