package one.mixin.android.widget.gallery.engine;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.widget.ImageView;

public interface ImageEngine {


    void loadThumbnail(Context context, int resize, Drawable placeholder, ImageView imageView, Uri uri);


    void loadGifThumbnail(Context context, int resize, Drawable placeholder, ImageView imageView, Uri uri);


    void loadImage(Context context, int resizeX, int resizeY, ImageView imageView, Uri uri);


    void loadGifImage(Context context, int resizeX, int resizeY, ImageView imageView, Uri uri);

    void loadWebp(Context context, ImageView imageView, Uri uri);

    boolean supportAnimatedGif();
}
