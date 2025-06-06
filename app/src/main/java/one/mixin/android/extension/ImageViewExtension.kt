package one.mixin.android.extension

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import coil3.dispose
import coil3.load
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.request.bitmapConfig
import coil3.request.error
import coil3.request.placeholder
import coil3.request.transformations
import coil3.transform.Transformation
import androidx.core.widget.TextViewCompat
import coil3.asDrawable
import coil3.imageLoader
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import jp.wasabeef.glide.transformations.CropTransformation
import jp.wasabeef.glide.transformations.RoundedCornersTransformation
import one.mixin.android.R
import one.mixin.android.util.StringSignature
import one.mixin.android.widget.CoilRoundedHexagonTransformation
import one.mixin.android.widget.lottie.RLottieDrawable
import one.mixin.android.widget.lottie.RLottieImageView

fun ImageView.loadImage(
    data: String?,
    @DrawableRes holder: Int? = null,
    base64Holder: String? = null,
    onSuccess: (
        (
        request: ImageRequest,
        result: SuccessResult,
    ) -> Unit
    )? = null,
    onError: ((request: ImageRequest, result: ErrorResult) -> Unit)? = null,
    transformation: Transformation? = null,
) {
    this.load(data) {
        if (base64Holder != null) {
            placeholder(base64Holder.toDrawable(layoutParams.width, layoutParams.height))
        } else if (holder != null) {
            placeholder(holder)
            error(holder)
        }
        allowHardware(false)
        if (transformation != null) transformations(transformation)
        onSuccess?.let {
            listener(
                onSuccess = onSuccess,
                onError = onError ?: { _, _ -> },
            )
        }
    }
}

fun ImageView.loadImageCompat(
    data: String?,
    @DrawableRes holder: Int? = null,
    base64Holder: String? = null,
    onSuccess: (
        (
        request: ImageRequest,
        result: SuccessResult,
    ) -> Unit
    )? = null,
    onError: ((request: ImageRequest, result: ErrorResult) -> Unit)? = null,
    transformation: Transformation? = null,
) {
    this.load(data) {
        if (base64Holder != null) {
            placeholder(base64Holder.toDrawable(layoutParams.width, layoutParams.height))
        } else if (holder != null) {
            placeholder(holder)
            error(holder)
        }
        allowHardware(false)
        bitmapConfig(Bitmap.Config.ARGB_8888)
        if (transformation != null) transformations(transformation)
        onSuccess?.let {
            listener(
                onSuccess = onSuccess,
                onError = onError ?: { _, _ -> },
            )
        }
    }
}

fun ImageView.loadImage(
    uri: Uri?,
    @DrawableRes holder: Int? = null,
    base64Holder: String? = null,
) {
    this.load(uri) {
        if (base64Holder != null) {
            placeholder(base64Holder.toDrawable(layoutParams.width, layoutParams.height))
        } else if (holder != null) {
            placeholder(holder)
            error(holder)
        }
        allowHardware(false)
    }
}

fun ImageView.loadSvgWithTint(url: String, isRising: Boolean, isColorReversed: Boolean) {
    val colorRes = when {
        isRising && !isColorReversed -> R.color.wallet_green
        isRising && isColorReversed -> R.color.wallet_pink
        !isRising && !isColorReversed -> R.color.wallet_pink
        else -> R.color.wallet_green
    }
    setColorFilter(ContextCompat.getColor(context, colorRes))
    load(url)
}

fun ImageView.clear() {
    this.dispose()
    Glide.with(this).clear(this)
}

fun ImageView.loadGifMark(
    uri: String?,
    holder: String?,
    mark: Int,
    useSignature: Boolean,
) {
    if (!isActivityNotDestroyed()) return
    var options = RequestOptions().dontTransform()
    if (useSignature) {
        options = options.signature(StringSignature("$uri$mark"))
    }
    if (holder != null) {
        options = options.placeholder(holder.toDrawable(layoutParams.width, layoutParams.height))
    }
    Glide.with(this).load(uri).apply(options).into(this)
}

fun ImageView.loadImageMark(
    uri: String?,
    holder: String?,
    mark: Int,
) {
    if (!isActivityNotDestroyed()) return
    Glide.with(this).load(uri).apply(
        RequestOptions().dontAnimate()
            .signature(StringSignature("$uri$mark")).run {
                return@run if (holder != null) {
                    this.placeholder(holder.toDrawable(layoutParams.width, layoutParams.height))
                } else {
                    this
                }
            },
    ).into(this)
}

fun ImageView.loadImageMark(
    uri: String?,
    @DrawableRes holder: Int?,
    mark: Int,
) {
    if (!isActivityNotDestroyed()) return
    Glide.with(this).load(uri).apply(
        RequestOptions().dontAnimate()
            .signature(StringSignature("$uri$mark")).run {
                return@run if (holder != null) {
                    this.placeholder(holder)
                } else {
                    this
                }
            },
    ).into(this)
}

fun ImageView.loadImageMark(
    uri: String?,
    mark: Int,
) {
    if (!isActivityNotDestroyed()) return
    Glide.with(this).load(uri).apply(
        RequestOptions().dontAnimate()
            .signature(StringSignature("$uri$mark")),
    )
        .listener(
            object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean,
                ): Boolean {
                    return true
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean,
                ): Boolean {
                    this@loadImageMark.context.runOnUiThread {
                        setImageDrawable(resource)
                    }
                    return true
                }
            },
        )
        .submit(layoutParams.width, layoutParams.height)
}

fun ImageView.loadLongImageMark(
    uri: String?,
    holder: String?,
    mark: Int,
) {
    if (!isActivityNotDestroyed()) return
    Glide.with(this).load(uri).apply(
        RequestOptions.bitmapTransform(
            CropTransformation(
                0,
                layoutParams.height,
                CropTransformation.CropType.TOP,
            ),
        )
            .dontAnimate()
            .signature(StringSignature("$uri$mark")).run {
                return@run if (holder != null) {
                    this.placeholder(holder.toDrawable(width, layoutParams.height))
                } else {
                    this
                }
            },
    ).into(this)
}

fun ImageView.loadLongImageMark(
    uri: String?,
    mark: Int?,
) {
    if (!isActivityNotDestroyed()) return
    Glide.with(this).load(uri).apply(
        RequestOptions.bitmapTransform(
            CropTransformation(
                0,
                layoutParams.height,
                CropTransformation.CropType.TOP,
            ),
        )
            .dontAnimate().run {
                return@run if (mark != null) {
                    signature(StringSignature("$uri$mark"))
                } else {
                    this
                }
            },
    ).listener(
        object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable>,
                isFirstResource: Boolean,
            ): Boolean {
                return true
            }

            override fun onResourceReady(
                resource: Drawable,
                model: Any,
                target: Target<Drawable>?,
                dataSource: DataSource,
                isFirstResource: Boolean,
            ): Boolean {
                this@loadLongImageMark.context.runOnUiThread {
                    setImageDrawable(resource)
                }
                return true
            }
        },
    ).submit(layoutParams.width, layoutParams.height)
}

fun ImageView.loadVideoMark(
    uri: String?,
    holder: String?,
    mark: Int,
) {
    if (!isActivityNotDestroyed()) return
    Glide.with(this).load(uri)
        .apply(
            RequestOptions().frame(0)
                .signature(StringSignature("$uri$mark"))
                .centerCrop().dontAnimate().run {
                    return@run if (holder != null) {
                        this.placeholder(holder.toDrawable(layoutParams.width, layoutParams.height))
                    } else {
                        this
                    }
                },
        ).listener(
            object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean,
                ): Boolean {
                    this@loadVideoMark.context.runOnUiThread {
                        holder?.toDrawable(width, height)?.let {
                            setImageDrawable(it)
                        }
                    }
                    return true
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean,
                ): Boolean {
                    this@loadVideoMark.context.runOnUiThread {
                        setImageDrawable(resource)
                    }
                    return true
                }
            },
        ).submit(layoutParams.width, layoutParams.height)
}

@SuppressLint("CheckResult")
fun ImageView.loadVideo(uri: String?) {
    if (!isActivityNotDestroyed()) return
    Glide.with(this).load(uri).apply(
        RequestOptions().frame(0)
            .centerCrop().dontAnimate(),
    ).into(this)
}

fun ImageView.loadVideo(
    uri: String?,
    holder: String?,
    width: Int,
    height: Int,
) {
    if (!isActivityNotDestroyed()) return
    Glide.with(this).load(uri)
        .apply(RequestOptions().placeholder(holder?.toDrawable(width, height)).override(width, height))
        .into(this)
}

fun RLottieImageView.loadSticker(
    url: String?,
    type: String?,
    cacheKey: String,
) {
    if (!isActivityNotDestroyed()) return
    url?.let {
        val imgType =
            type?.uppercase() ?: try {
                url.substring(url.lastIndexOf("." + 1)).uppercase()
            } catch (e: Exception) {
                null
            }
        clear()
        when (imgType) {
            "JSON" -> loadLottie(it, cacheKey)
            else -> loadImage(url, null, null)
        }
    }
}

fun RLottieImageView.loadLottie(
    uri: String,
    cacheKey: String,
) {
    if (!isActivityNotDestroyed()) return
    Glide.with(this)
        .`as`(RLottieDrawable::class.java)
        .load(uri).apply(
            RequestOptions().dontAnimate()
                .signature(StringSignature(cacheKey)),
        )
        .into(this)
}

fun ImageView.loadRoundImage(
    uri: String?,
    radius: Int,
    @DrawableRes holder: Int? = null,
) {
    if (!isActivityNotDestroyed()) return
    if (uri.isNullOrBlank() && holder != null) {
        setImageResource(holder)
    } else if (holder == null) {
        Glide.with(this).load(uri)
            .apply(RequestOptions.bitmapTransform(RoundedCornersTransformation(radius, 0)))
            .into(this)
    } else {
        Glide.with(this).load(uri).apply(
            RequestOptions().transform(RoundedCornersTransformation(radius, 0))
                .placeholder(holder),
        )
            .into(this)
    }
}

fun TextView.loadImage(data: Any?, size: Int, @DrawableRes placeholder: Int? = null) {
    val request = ImageRequest.Builder(context).data(data).apply {
        placeholder?.let { placeholder(it) }
        transformations(CoilRoundedHexagonTransformation())
    }.target { image ->
        val drawable = image.asDrawable(this.context.resources)
        drawable.setBounds(0, 0, size, size)
        TextViewCompat.setCompoundDrawablesRelative(this, drawable, null, null, null)
    }.build()
    context.imageLoader.enqueue(request)
}