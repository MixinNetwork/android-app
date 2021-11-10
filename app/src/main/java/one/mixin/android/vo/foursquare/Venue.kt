package one.mixin.android.vo.foursquare

import com.squareup.moshi.JsonClass
import java.util.regex.Pattern

@JsonClass(generateAdapter = true)
class Venue(
    val id: String,
    val name: String,
    val location: Location,
    val categories: List<Category>?
)

fun Venue.getImageUrl(): String? {
    if (categories.isNullOrEmpty()) return null
    return categories[0].icon.run {
        "${prefix}64$suffix"
    }
}

fun Venue.getVenueType(): String? {
    if (categories.isNullOrEmpty()) return null
    return categories[0].icon.run {
        val matcher = venuePattern.matcher(prefix)
        if (matcher.find()) {
            val start = matcher.start() + 14
            val end = matcher.end() - 1
            if (start < end) {
                return prefix.substring(start, end)
            }
        }
        null
    }
}

private val venuePattern by lazy { Pattern.compile("categories_v2/\\S+/\\S+\$") }
