import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import one.mixin.android.ui.home.inscription.component.ShareBottom
import one.mixin.android.ui.home.web3.components.InscriptionState

@Composable
fun SharePage(
    qrcode: Bitmap, inscriptionHash: String, value: InscriptionState, inScreenshot: Boolean,
    onClose: () -> Unit, onCopy: () -> Unit, onSave: (size: IntSize, bottomSize: IntSize) -> Unit, onShare: (size: IntSize, bottomSize: IntSize) -> Unit) {
    val targetSize = remember { mutableStateOf(IntSize.Zero) }
    Box(
        modifier = Modifier
            .background(Color(0xB3000000))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                onClose()
            }
    ) {
        BackHandler {
            onClose()
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(20.dp)
                .align(Alignment.Center)
        ) {
            ShareCard(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .wrapContentHeight()
                    .onGloballyPositioned { coordinates ->
                        targetSize.value = coordinates.size
                    }, qrcode = qrcode, inscriptionHash, value, inScreenshot
            ) {
                onClose()
            }
            Spacer(modifier = Modifier.height(12.dp))
            ShareBottom(modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .padding(16.dp), onShare = { bottomSize ->
                onShare(targetSize.value, bottomSize)
            }, onCopy = onCopy, onSave = { bottomSize ->
                onSave(targetSize.value, bottomSize)
            })
        }
    }
}