package one.mixin.android.extension

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import jp.wasabeef.glide.transformations.CropTransformation
import jp.wasabeef.glide.transformations.RoundedCornersTransformation
import one.mixin.android.util.StringSignature
import org.jetbrains.anko.runOnUiThread

fun ImageView.loadImage(uri: String?) {
    if (!isActivityNotDestroyed()) return
    Glide.with(this).load(uri).into(this)
}

fun ImageView.loadImage(uri: Uri?) {
    if (!isActivityNotDestroyed()) return
    Glide.with(this).load(uri).into(this)
}

fun ImageView.loadImage(uri: String?, @DrawableRes holder: Int) {
    if (!isActivityNotDestroyed()) return
    Glide.with(this).load(uri).apply(RequestOptions.placeholderOf(holder)).into(this)
}

fun ImageView.clear() {
    Glide.with(this).clear(this)
}

fun ImageView.loadImage(uri: String?, width: Int, height: Int) {
    if (!isActivityNotDestroyed()) return
    val multi = MultiTransformation(CropTransformation(width, height))
    Glide.with(this).load(uri).apply(RequestOptions.bitmapTransform(multi).dontAnimate()).into(this)
}

fun ImageView.loadBase64ImageCenterCrop(imageByteArray: ByteArray?, @DrawableRes holder: Int? = null) {
    if (!isActivityNotDestroyed()) return
    Glide.with(this).asBitmap().load(imageByteArray)
        .apply(
            RequestOptions().dontAnimate().dontTransform().centerCrop().apply {
                if (holder != null) {
                    this.placeholder(holder)
                }
            }
        ).into(this)
}

fun ImageView.loadImageCenterCrop(uri: String?, @DrawableRes holder: Int? = null) {
    if (!isActivityNotDestroyed()) return
    Glide.with(this).load(uri)
        .apply(
            RequestOptions().dontAnimate().dontTransform().centerCrop().apply {
                if (holder != null) {
                    this.placeholder(holder)
                }
            }
        ).into(this)
}

fun ImageView.loadImageCenterCrop(uri: Uri?, @DrawableRes holder: Int? = null) {
    if (!isActivityNotDestroyed()) return
    Glide.with(this).load(uri)
        .apply(
            RequestOptions().dontAnimate().dontTransform().centerCrop().apply {
                if (holder != null) {
                    this.placeholder(holder)
                }
            }
        ).into(this)
}

fun ImageView.loadImage(
    uri: String?,
    base64Holder: String? = null,
    requestListener: RequestListener<Drawable?>? = null,
    overrideWidth: Int? = null,
    overrideHeight: Int? = null
) {
    if (!isActivityNotDestroyed()) return
    var requestOptions = RequestOptions().dontTransform()
    if (base64Holder != null) {
        requestOptions = requestOptions.fallback(base64Holder.toDrawable(width, height))
    }
    if (overrideWidth != null && overrideHeight != null) {
        requestOptions = requestOptions.override(overrideWidth, overrideHeight)
    }
    if (requestListener != null) {
        Glide.with(this).load(uri).apply(requestOptions).listener(requestListener)
            .into(this)
    } else {
        Glide.with(this).load(uri).apply(requestOptions).into(this)
    }
}

fun ImageView.loadGif(
    uri: String?,
    requestListener: RequestListener<GifDrawable?>? = null,
    centerCrop: Boolean? = null,
    @DrawableRes holder: Int? = null,
    base64Holder: String? = null,
    overrideWidth: Int? = null,
    overrideHeight: Int? = null
) {
    if (!isActivityNotDestroyed()) return
    var requestOptions = RequestOptions().dontTransform()
    if (centerCrop != null) {
        requestOptions = requestOptions.centerCrop()
    }
    if (holder != null) {
        requestOptions = requestOptions.placeholder(holder)
    }
    if (base64Holder != null) {
        requestOptions = requestOptions.fallback(base64Holder.toDrawable(width, height))
    }
    if (overrideWidth != null && overrideHeight != null) {
        requestOptions = requestOptions.override(overrideWidth, overrideHeight)
    }
    if (requestListener != null) {
        Glide.with(this).asGif().load(uri).apply(requestOptions).listener(requestListener)
            .into(this)
    } else {
        Glide.with(this).asGif().load(uri).apply(requestOptions).into(this)
    }
}

fun ImageView.loadGifMark(uri: String?, holder: String?, mark: Int) {
    if (!isActivityNotDestroyed()) return
    Glide.with(this).asGif().load(uri).apply(
        RequestOptions().dontTransform()
            .signature(StringSignature("$uri$mark")).apply {
                if (holder != null) {
                    this.placeholder(holder.toDrawable(width, height))
                }
            }
    ).into(this)
}

fun ImageView.loadGifMark(uri: String?, mark: Int, useSignature: Boolean = true) {
    if (!isActivityNotDestroyed()) return
    var options = RequestOptions().dontTransform()
    if (useSignature) {
        options = options.signature(StringSignature("$uri$mark"))
    }
    Glide.with(this).asGif().load(uri).apply(options)
        .listener(object : RequestListener<GifDrawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<GifDrawable>?,
                isFirstResource: Boolean
            ): Boolean {
                return true
            }

            override fun onResourceReady(
                resource: GifDrawable?,
                model: Any?,
                target: Target<GifDrawable>?,
                dataSource: DataSource?,
                isFirstResource: Boolean
            ): Boolean {
                this@loadGifMark.context.runOnUiThread {
                    setImageDrawable(resource)
                }
                return true
            }
        })
        .submit(layoutParams.width, layoutParams.height)
}

fun ImageView.loadImageMark(uri: String?, holder: String?, mark: Int) {
    if (!isActivityNotDestroyed()) return
    Glide.with(this).load(uri).apply(
        RequestOptions().dontAnimate()
            .signature(StringSignature("$uri$mark")).apply {
                if (holder != null) {
                    this.placeholder(holder.toDrawable(width, height))
                }
            }
    ).into(this)
}

fun ImageView.loadImageMark(uri: String?, @DrawableRes holder: Int?, mark: Int) {
    if (!isActivityNotDestroyed()) return
    Glide.with(this).load(uri).apply(
        RequestOptions().dontAnimate()
            .signature(StringSignature("$uri$mark")).apply {
                if (holder != null) {
                    this.placeholder(holder)
                }
            }
    ).into(this)
}

fun ImageView.loadImageMark(uri: String?, mark: Int) {
    if (!isActivityNotDestroyed()) return
    Glide.with(this).load(uri).apply(
        RequestOptions().dontAnimate()
            .signature(StringSignature("$uri$mark"))
    )
        .listener(object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable>?,
                isFirstResource: Boolean
            ): Boolean {
                return true
            }

            override fun onResourceReady(
                resource: Drawable?,
                model: Any?,
                target: Target<Drawable>?,
                dataSource: DataSource?,
                isFirstResource: Boolean
            ): Boolean {
                this@loadImageMark.context.runOnUiThread {
                    setImageDrawable(resource)
                }
                return true
            }
        })
        .submit(layoutParams.width, layoutParams.height)
}

fun ImageView.loadLongImageMark(uri: String?, holder: String?, mark: Int) {
    if (!isActivityNotDestroyed()) return
    Glide.with(this).load(uri).apply(
        RequestOptions.bitmapTransform(
            CropTransformation(
                0,
                layoutParams.height,
                CropTransformation.CropType.TOP
            )
        )
            .dontAnimate()
            .signature(StringSignature("$uri$mark")).apply {
                if (holder != null) {
                    this.placeholder(holder.toDrawable(width, height))
                }
            }
    ).into(this)
}

fun ImageView.loadLongImageMark(uri: String?, mark: Int) {
    if (!isActivityNotDestroyed()) return
    Glide.with(this).load(uri).apply(
        RequestOptions.bitmapTransform(
            CropTransformation(
                0,
                layoutParams.height,
                CropTransformation.CropType.TOP
            )
        )
            .dontAnimate()
            .signature(StringSignature("$uri$mark"))
    ).listener(object : RequestListener<Drawable> {
        override fun onLoadFailed(
            e: GlideException?,
            model: Any?,
            target: Target<Drawable>?,
            isFirstResource: Boolean
        ): Boolean {
            return true
        }

        override fun onResourceReady(
            resource: Drawable?,
            model: Any?,
            target: Target<Drawable>?,
            dataSource: DataSource?,
            isFirstResource: Boolean
        ): Boolean {
            this@loadLongImageMark.context.runOnUiThread {
                setImageDrawable(resource)
            }
            return true
        }
    }).submit(layoutParams.width, layoutParams.height)
}

fun ImageView.loadVideoMark(
    uri: String?,
    holder: String?,
    mark: Int
) {
    if (!isActivityNotDestroyed()) return
    Glide.with(this).load(uri)
        .apply(
            RequestOptions().frame(0)
                .signature(StringSignature("$uri$mark"))
                .centerCrop().dontAnimate().apply {
                    if (holder != null) {
                        this.placeholder(holder.toDrawable(width, height))
                    }
                }
        ).listener(object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable>?,
                isFirstResource: Boolean
            ): Boolean {
                this@loadVideoMark.context.runOnUiThread {
                    holder?.toDrawable(width, height)?.let {
                        setImageDrawable(it)
                    }
                }
                return true
            }

            override fun onResourceReady(
                resource: Drawable?,
                model: Any?,
                target: Target<Drawable>?,
                dataSource: DataSource?,
                isFirstResource: Boolean
            ): Boolean {
                this@loadVideoMark.context.runOnUiThread {
                    setImageDrawable(resource)
                }
                return true
            }
        }).submit(layoutParams.width, layoutParams.height)
}

@SuppressLint("CheckResult")
fun ImageView.loadVideo(uri: String?) {
    if (!isActivityNotDestroyed()) return
    Glide.with(this).load(uri).apply(
        RequestOptions().frame(0)
            .centerCrop().dontAnimate()
    ).into(this)
}

fun ImageView.loadVideo(uri: String?, holder: String?, width: Int, height: Int) {
    if (!isActivityNotDestroyed()) return
    Glide.with(this).load(uri).apply {
        RequestOptions().placeholder(holder?.toDrawable(width, height)).override(width, height)
    }.into(this)
}

fun ImageView.loadSticker(uri: String?, type: String?) {
    if (!isActivityNotDestroyed()) return
    uri?.let {
        when (type) {
            "GIF" -> {
                loadGif(uri)
            }
            else -> loadImage(uri)
        }
    }
}

fun ImageView.loadBase64(uri: ByteArray?, width: Int, height: Int, mark: Int) {
    if (!isActivityNotDestroyed()) return
    val multi = MultiTransformation(CropTransformation(width, height))
    Glide.with(this).load(uri)
        .apply(
            RequestOptions().centerCrop()
                .transform(multi).signature(StringSignature("$uri$mark"))
                .dontAnimate()
        ).into(this)
}

fun ImageView.loadCircleImage(uri: String?, @DrawableRes holder: Int? = null) {
    if (!isActivityNotDestroyed()) return
    if (uri.isNullOrBlank()) {
        if (holder != null) {
            setImageResource(holder)
        }
    } else if (holder == null) {
        Glide.with(this).load(uri).apply(RequestOptions().circleCrop()).into(this)
    } else {
        Glide.with(this).load(uri).apply(RequestOptions().placeholder(holder).circleCrop())
            .into(this)
    }
}

fun ImageView.loadRoundImage(uri: String?, radius: Int, @DrawableRes holder: Int? = null) {
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
                .placeholder(holder)
        )
            .into(this)
    }
}
