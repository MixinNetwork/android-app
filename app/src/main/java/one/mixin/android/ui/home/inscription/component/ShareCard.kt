package one.mixin.android.ui.home.inscription.component

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.request.ImageRequest
import one.mixin.android.R
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.CoilImageCompat
import one.mixin.android.inscription.compose.Barcode
import one.mixin.android.inscription.compose.TextInscription
import one.mixin.android.ui.home.web3.components.InscriptionState
import one.mixin.android.widget.CoilRoundedHexagonTransformation

@Composable
fun ShareCard(modifier: Modifier, qrcode: Bitmap, inscriptionHash: String, value: InscriptionState, inScreenshot: Boolean, onClose: () -> Unit) {
    Column(
        modifier = modifier
    ) {
        Box {
            CoilImageCompat(
                model = value.contentURL,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                placeholder = if (value.isText) R.drawable.bg_text_inscirption else R.drawable.ic_inscription_content,
            )
            if (value.isText) {
                TextInscription(
                    value.iconUrl, value.contentURL,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                )
            } else {
                CoilImageCompat(
                    model = value.contentURL,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    placeholder = R.drawable.ic_inscription_content,
                )
            }
            Box(
                modifier = Modifier
                    .alpha(if (inScreenshot) 0f else 1f)
                    .align(Alignment.TopEnd)
                    .padding(top = 11.dp, end = 9.dp)
            ) {
                Image(
                    modifier = Modifier
                        .padding(2.dp)
                        .clickable { onClose() },
                    painter = painterResource(id = R.drawable.ic_float_close), contentDescription = null,
                )
            }
        }
        Row(modifier = Modifier.padding(20.dp)) {
            Column(modifier = Modifier.height(110.dp)) {
                Text(text = value.name ?: "", fontSize = 18.sp, color = Color.Black, maxLines = 2)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = value.id, fontSize = 12.sp, color = Color(0xFFBBBEC3))
                Spacer(modifier = Modifier.weight(1f))
                Barcode(
                    inscriptionHash,
                    modifier = Modifier
                        .width(132.dp)
                        .height(30.dp),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Box {
                Image(
                    bitmap = qrcode.asImageBitmap(),
                    modifier = Modifier.size(110.dp),
                    contentDescription = null
                )

                CoilImage(
                    model =
                    ImageRequest.Builder(LocalContext.current)
                        .data(value.iconUrl)
                        .transformations(CoilRoundedHexagonTransformation())
                        .build(),
                    modifier = Modifier
                        .width(24.dp)
                        .height(24.dp)
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(4.dp)),
                    placeholder = R.drawable.ic_inscription_icon,
                )
            }
        }
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            DashedDivider(
                thickness = 1.dp,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row {
                Column(modifier = Modifier.height(36.dp)) {
                    Text(text = "Mixin Collectibles", fontSize = 15.sp, color = Color.Black)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(text = "inscription.mixin.space", fontSize = 12.sp, color = Color(0xFFBBBEC3))
                }
                Spacer(modifier = Modifier.weight(1f))
                Image(painter = painterResource(id = R.drawable.ic_collectibles_logo), contentDescription = null)
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Preview
@Composable
private fun DashedDividerPreview() {
    DashedDivider(
        thickness = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    )
}

@Composable
fun DashedDivider(
    thickness: Dp,
    color: Color = Color(0xFFBBBEC3),
    phase: Float = 6.dp.value,
    intervals: FloatArray = floatArrayOf(15.dp.value, 15.dp.value),
    modifier: Modifier
) {
    Canvas(
        modifier = modifier
    ) {
        val dividerHeight = thickness.toPx()
        drawRoundRect(
            color = color,
            style = Stroke(
                width = dividerHeight,
                pathEffect = PathEffect.dashPathEffect(
                    intervals = intervals,
                    phase = phase
                )
            )
        )
    }
}