package one.mixin.android.vo.foursquare

class Venues(
    val id: String,
    val name: String,
    val location: Location,
    val categories: List<Category>?
)

fun Venues.getImageUrl(): String? {
    if (categories.isNullOrEmpty()) return null
    return categories[0].icon.run {
        "${prefix}88$suffix"
    }
}
