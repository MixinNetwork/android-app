package one.mixin.android.compose

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.compose.rememberImagePainter
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest

@Composable
fun CoilImage(
    model: String?,
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
