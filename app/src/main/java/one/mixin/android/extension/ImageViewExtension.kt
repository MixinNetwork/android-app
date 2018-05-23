package one.mixin.android.extension

import android.graphics.drawable.Drawable
import android.support.annotation.DrawableRes
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import jp.wasabeef.glide.transformations.CropTransformation
import jp.wasabeef.glide.transformations.MaskTransformation
import jp.wasabeef.glide.transformations.RoundedCornersTransformation
import one.mixin.android.util.StringSignature

fun ImageView.loadImage(uri: String?, width: Int, height: Int) {
    val multi = MultiTransformation(CropTransformation(width, height))
    Glide.with(this).load(uri).apply(RequestOptions.bitmapTransform(multi).dontAnimate()).into(this)
}

fun ImageView.loadImage(uri: String?, requestListener: RequestListener<Drawable?>) {
    Glide.with(this).load(uri).listener(requestListener).into(this)
}

fun ImageView.loadGif(uri: String?, requestListener: RequestListener<GifDrawable?>? = null) {
    if (requestListener != null) {
        Glide.with(this).asGif().load(uri).listener(requestListener).into(this)
    } else {
        Glide.with(this).load(uri).into(this)
    }
}

fun ImageView.loadImage(uri: String?, @DrawableRes holder: Int? = null) {
    if (uri.isNullOrBlank()) {
        if (holder != null) {
            setImageResource(holder)
        }
    } else if (holder == null) {
        Glide.with(this).load(uri).apply(RequestOptions().dontAnimate()).into(this)
    } else {
        Glide.with(this).load(uri).apply(RequestOptions().placeholder(holder).dontAnimate()).into(this)
    }
}

fun ImageView.loadSticker(uri: String?) {
    uri?.let {
        Glide.with(this).load(it).into(this)
    }
}

fun ImageView.loadImageUseMark(uri: String?, @DrawableRes holder: Int, @DrawableRes mark: Int? = null) {
    when {
        uri.isNullOrBlank() -> setImageResource(holder)
        mark == null -> loadImage(uri, holder)
        else -> Glide.with(this).load(uri).apply(RequestOptions().centerCrop().transform(MaskTransformation(mark))
            .signature(StringSignature("$uri$mark"))
            .placeholder(holder).dontAnimate()
        ).into(this)
    }
}

fun ImageView.loadVideoUseMark(uri: String, @DrawableRes holder: Int, @DrawableRes mark: Int) {
    Glide.with(this).load(uri).apply(RequestOptions().frame(0).centerCrop().transform(MaskTransformation(mark))
        .signature(StringSignature("$uri$mark"))
        .placeholder(holder).dontAnimate()
    ).into(this)
}

fun ImageView.loadImage(uri: ByteArray?, width: Int, height: Int, @DrawableRes mark: Int? = null) {
    val multi = MultiTransformation(CropTransformation(width, height))
    if (mark == null) {
        Glide.with(this).load(uri).apply(RequestOptions().centerCrop().transform(multi).dontAnimate()).into(this)
    } else {
        Glide.with(this).load(uri).apply(RequestOptions().centerCrop().transform(multi).transform(MaskTransformation(mark))
            .dontAnimate())
            .into(this)
    }
}

fun ImageView.loadCircleImage(uri: String?, @DrawableRes holder: Int? = null) {
    if (uri.isNullOrBlank()) {
        if (holder != null) {
            setImageResource(holder)
        }
    } else if (holder == null) {
        Glide.with(this).load(uri).apply(RequestOptions().circleCrop()).into(this)
    } else {
        Glide.with(this).load(uri).apply(RequestOptions().placeholder(holder).circleCrop()).into(this)
    }
}

fun ImageView.loadRoundImage(uri: String?, radius: Int, @DrawableRes holder: Int? = null) {
    if (uri.isNullOrBlank() && holder != null) {
        setImageResource(holder)
    } else if (holder == null) {
        Glide.with(this).load(uri).apply(RequestOptions.bitmapTransform(RoundedCornersTransformation(radius, 0))).into(this)
    } else {
        Glide.with(this).load(uri).apply(RequestOptions().transform(RoundedCornersTransformation(radius, 0))
            .placeholder(holder))
            .into(this)
    }
}