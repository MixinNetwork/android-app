package one.mixin.android.ui.home.inscription.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R

@Composable
fun ShareBottom(modifier: Modifier, onShare: (IntSize) -> Unit, onCopy: () -> Unit, onSave: (IntSize) -> Unit) {
    val bottomSize = remember { mutableStateOf(IntSize.Zero) }

    Row(
        modifier = modifier
            .wrapContentHeight()
            .onGloballyPositioned { coordinates ->
                bottomSize.value = coordinates.size
            }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {
                    onShare(bottomSize.value)
                }),
        ) {
            Image(painter = painterResource(id = R.drawable.ic_inscription_share), contentDescription = null)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = stringResource(id = R.string.Share), fontSize = 12.sp, color = Color.White)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {
                    onCopy()
                }),
        ) {
            Image(painter = painterResource(id = R.drawable.ic_inscirption_copy), contentDescription = null)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = stringResource(id = R.string.Copy), fontSize = 12.sp, color = Color.White)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {
                    onSave(bottomSize.value)
                }),
        ) {
            Image(painter = painterResource(id = R.drawable.ic_inscription_save), contentDescription = null)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = stringResource(id = R.string.Save), fontSize = 12.sp, color = Color.White)
        }
    }
}