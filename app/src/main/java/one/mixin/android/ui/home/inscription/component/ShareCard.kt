import android.graphics.Bitmap
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.request.ImageRequest
import one.mixin.android.R
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.CoilImageCompat
import one.mixin.android.inscription.compose.Barcode
import one.mixin.android.ui.home.web3.components.InscriptionState
import one.mixin.android.widget.CoilRoundedHexagonTransformation

@Composable
fun ShareCard(modifier: Modifier, qrcode: Bitmap, inscriptionHash: String, value: InscriptionState) {

    Column(
        modifier = modifier
    ) {
        CoilImageCompat(
            model = value.contentURL,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            placeholder = R.drawable.ic_inscription_content,
        )
        Row(modifier = Modifier.padding(20.dp)) {
            Column(modifier = Modifier.height(100.dp)) {
                Text(text = value.name ?: "", fontSize = 18.sp, color = Color.Black, maxLines = 2)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = value.id, fontSize = 12.sp, color = Color(0xFF333333))
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
    }
}