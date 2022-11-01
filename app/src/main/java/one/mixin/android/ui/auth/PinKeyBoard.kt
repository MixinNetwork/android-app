package one.mixin.android.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import one.mixin.android.R
import one.mixin.android.extension.pxToDp
import one.mixin.android.extension.tickVibrate
import one.mixin.android.ui.setting.ui.theme.MixinAppTheme

@Composable
fun PinKeyBoard(
    callback: (String) -> Unit
) {
    MixinAppTheme {
        val context = LocalContext.current
        var size by remember { mutableStateOf(IntSize.Zero) }

        val list = listOf(
            "1", "2", "3",
            "4", "5", "6",
            "7", "8", "9",
            "", "0", "<"
        )
        Column(
            modifier = Modifier
                .background(MixinAppTheme.colors.backgroundWindow)
                .fillMaxHeight()
        ) {
            Box(
                modifier = Modifier
                    .background(MixinAppTheme.colors.backgroundGray)
                    .height(36.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    color = MixinAppTheme.colors.textMinor,
                    text = stringResource(id = R.string.Secured_by_TIP),
                    fontSize = 12.sp
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .onSizeChanged {
                        size = it
                    }
            ) {
                LazyVerticalGrid(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                    columns = GridCells.Fixed(3),
                    content = {
                        items(list.size) { index ->
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .height(context.pxToDp(size.toSize().height / 4).dp - 1.dp)
                                    .background(MixinAppTheme.colors.background)
                                    .run {
                                        if (index != 9) {
                                            clickable {
                                                context.tickVibrate()
                                                callback(list[index])
                                            }
                                        } else {
                                            this
                                        }
                                    }
                            ) {
                                if (index == 11) {
                                    Image(
                                        painter = painterResource(R.drawable.ic_delete),
                                        contentDescription = null
                                    )
                                } else {
                                    Text(
                                        text = list[index],
                                        fontSize = 25.sp,
                                        color = MixinAppTheme.colors.textPrimary,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Preview
@Composable
fun PinKeyBoardPreview() {
    PinKeyBoard {
        println(it)
    }
}
