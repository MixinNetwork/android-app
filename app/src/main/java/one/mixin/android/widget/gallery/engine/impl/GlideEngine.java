package one.mixin.android.widget.gallery.engine.impl;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.request.RequestOptions;
import one.mixin.android.widget.gallery.engine.ImageEngine;

public class GlideEngine implements ImageEngine {

    @Override
    public void loadThumbnail(Context context, int resize, Drawable placeholder, ImageView imageView,
                              Uri uri) {
        Glide.with(context)
                .asBitmap()
                .load(uri)
                .apply(RequestOptions.placeholderOf(placeholder).override(resize, resize).centerCrop())
                .into(imageView);
    }

    @Override
    public void loadGifThumbnail(Context context, int resize, Drawable placeholder,
                                 ImageView imageView, Uri uri) {
        Glide.with(context)
                .asBitmap()
                .load(uri)
                .apply(RequestOptions.placeholderOf(placeholder).override(resize, resize).centerCrop())
                .into(imageView);
    }

    @Override
    public void loadImage(Context context, int resizeX, int resizeY, ImageView imageView, Uri uri) {
        Glide.with(context)
                .asBitmap()
                .load(uri)
                .apply(RequestOptions.overrideOf(resizeX, resizeY).priority(Priority.HIGH).fitCenter())
                .into(imageView);
    }

    @Override
    public void loadGifImage(Context context, int resizeX, int resizeY, ImageView imageView,
                             Uri uri) {
        Glide.with(context)
                .asGif()
                .load(uri)
                .apply(RequestOptions.overrideOf(resizeX, resizeY).priority(Priority.HIGH).dontTransform())
                .into(imageView);
    }

    @Override
    public void loadWebp(Context context, ImageView imageView, Uri uri) {
        Glide.with(context).load(uri).into(imageView);
    }

    @Override
    public boolean supportAnimatedGif() {
        return true;
    }
}
