import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import one.mixin.android.R
import timber.log.Timber
import java.io.File

@Composable
fun GlideImage(
    modifier: Modifier = Modifier,
    data: Any,
    glideModifier: (RequestBuilder<Drawable>) -> RequestBuilder<Drawable> = {
        it
    },
    placeHolderPainter: Painter? = null,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Fit,
) {
    if (LocalInspectionMode.current) {
        Image(
            painter = painterResource(id = R.drawable.ic_transfer_address),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
        )
        return
    }
    BoxWithConstraints(modifier = modifier) {
        val state =
            remember(placeHolderPainter) {
                mutableStateOf<ImageBitmap?>(null)
            }
        val context = LocalContext.current
        DisposableEffect(data, modifier, glideModifier, placeHolderPainter) {
            val glide = Glide.with(context)
            var builder =
                when (data) {
                    is Int -> {
                        glide.load(data)
                    }
                    is Uri -> {
                        glide.load(data)
                    }
                    is File -> {
                        glide.load(data)
                    }
                    is Drawable -> {
                        glide.load(data)
                    }
                    is ByteArray -> {
                        glide.load(data)
                    }
                    is Bitmap -> {
                        glide.load(data)
                    }
                    is String -> {
                        glide.load(data)
                    }
                    else -> {
                        glide.load(data)
                    }
                }
            builder = glideModifier(builder)
            val request =
                builder.into(
                    object : CustomTarget<Drawable>(
                        constraints.maxWidth,
                        constraints.maxHeight,
                    ) {
                        override fun onResourceReady(
                            resource: Drawable,
                            transition: Transition<in Drawable>?,
                        ) {
                            try {
                                state.value = resource.toBitmap().asImageBitmap()
                            } catch (e: Exception) {
                                Timber.e(e)
                            }
                        }

                        override fun onLoadStarted(placeholder: Drawable?) {
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                        }
                    },
                ).request!!
            onDispose {
                request.clear()
            }
        }
        val currentBitmap = state.value
        if (currentBitmap != null) {
            Image(
                modifier = modifier,
                contentDescription = contentDescription,
                painter = BitmapPainter(currentBitmap),
                contentScale = contentScale,
            )
        } else if (placeHolderPainter != null) {
            Image(
                modifier = modifier,
                contentDescription = contentDescription,
                painter = placeHolderPainter,
                contentScale = contentScale,
            )
        }
    }
}
