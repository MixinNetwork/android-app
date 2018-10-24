package one.mixin.android.vo.giphy

import com.google.gson.annotations.SerializedName

class ImageSet(
    @SerializedName("fixed_height")
    val fixed_height: Image,

    @SerializedName("fixed_height_still")
    val fixed_height_still: Image,

    @SerializedName("fixed_height_downsampled")
    val fixed_height_downsampled: Image,

    @SerializedName("fixed_width")
    val fixed_width: Image,

    @SerializedName("fixed_width_still")
    val fixed_width_still: Image,

    @SerializedName("fixed_width_downsampled")
    val fixed_width_downsampled: Image,

    @SerializedName("fixed_height_small")
    val fixed_height_small: Image,

    @SerializedName("fixed_height_small_sill")
    val fixed_height_small_sill: Image,

    @SerializedName("fixed_width_small")
    val fixed_width_small: Image,

    @SerializedName("fixed_width_small_still")
    val fixed_width_small_still: Image,

    @SerializedName("downsized")
    val downsized: Image,
    @SerializedName("downsized_still")
    val downsized_still: Image,

    @SerializedName("downsized_large")
    val downsized_large: Image,

    @SerializedName("original")
    val original: Image,

    @SerializedName("original_still")
    val original_still: Image
)