package one.mixin.android.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest

@Composable
fun CoilImage(model: String?, placeholder: Int, modifier: Modifier, contentScale: ContentScale = ContentScale.Fit) {
    AsyncImage(
        modifier = modifier,
        model = ImageRequest.Builder(LocalContext.current)
            .data(model)
            .decoderFactory(SvgDecoder.Factory())
            .build(),
        contentDescription = null,
        placeholder = painterResource(id = placeholder),
        contentScale = contentScale
    )
}

@Composable
fun CoilImage(model: ImageRequest, placeholder: Int, modifier: Modifier, contentScale: ContentScale = ContentScale.Fit) {
    AsyncImage(
        modifier = modifier,
        model = model,
        contentDescription = null,
        placeholder = painterResource(id = placeholder),
        contentScale = contentScale
    )
}