package one.mixin.android.compose

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest

@Composable
fun CoilImage(
    model: String?,
    placeholder: Int?,
    modifier: Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    AsyncImage(
        modifier = modifier,
        model = model,
        contentDescription = null,
        placeholder = placeholder?.let { painterResource(id = placeholder) },
        error = placeholder?.let { painterResource(id = placeholder) },
        contentScale = contentScale,
    )
}

@Composable
fun CoilImage(
    model: String?,
    placeholder: Bitmap?,
    modifier: Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    AsyncImage(
        modifier = modifier,
        model = model,
        contentDescription = null,
        placeholder = placeholder?.let { rememberAsyncImagePainter(placeholder) },
        error = placeholder?.let { rememberAsyncImagePainter(placeholder) },
        contentScale = contentScale,
    )
}

@Composable
fun CoilImageCompat(
    model: String?,
    placeholder: Int,
    modifier: Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val context = LocalContext.current
    val imageRequest = ImageRequest.Builder(context)
        .data(model)
        .allowHardware(false)
        .placeholder(placeholder)
        .error(placeholder)
        .build()

    val painter = rememberAsyncImagePainter(model = imageRequest)

    Image(
        modifier = modifier,
        painter = painter,
        contentScale = contentScale,
        contentDescription = null,
    )
}

@Composable
fun CoilImage(
    model: ImageRequest,
    placeholder: Int,
    modifier: Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    AsyncImage(
        modifier = modifier,
        model = model,
        contentDescription = null,
        placeholder = painterResource(id = placeholder),
        error = painterResource(id = placeholder),
        contentScale = contentScale,
    )
}
