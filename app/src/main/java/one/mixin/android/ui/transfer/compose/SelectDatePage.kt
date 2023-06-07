package one.mixin.android.ui.transfer.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.ui.setting.ui.compose.MixinBackButton
import one.mixin.android.ui.setting.ui.compose.MixinTopAppBar
import one.mixin.android.ui.setting.ui.theme.MixinAppTheme

@Composable
fun SelectDatePage() {
    var dateSelect by remember {
        mutableStateOf(false)
    }
    val dateValue by remember {
        mutableStateOf(6)
    }

    Scaffold(
        backgroundColor = MixinAppTheme.colors.background,
        topBar = {
            val context = LocalContext.current
            MixinTopAppBar(
                navigationIcon = {
                    MixinBackButton()
                },
                title = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(text = stringResource(id = R.string.Date))
                    }
                },
                actions = {
                    val context = LocalContext.current
                    IconButton(onClick = {
                        // todo
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_check_blue_24dp),
                            contentDescription = null,
                            tint = MixinAppTheme.colors.accent,
                        )
                    }
                },
            )
        },
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MixinAppTheme.colors.backgroundGray),
        ) {
            SelectItem(stringResource(id = R.string.all_time), !dateSelect) {
                dateSelect = false
            }
            SelectItem(stringResource(id = R.string.Specified_time_period), dateSelect) {
                dateSelect = true
            }
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                YearMothSwitch()
            }
        }
    }
}

@Composable
fun SelectItem(
    title: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Tile(
        title = title,
        trailing = {
            if (selected) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_check_blue_24dp),
                    contentDescription = null,
                    tint = MixinAppTheme.colors.accent,
                )
            }
        },
        onClick = {
            if (!selected) {
                onSelect()
            }
        },
    )
}

@Composable
fun Tile(
    trailing: @Composable () -> Unit = {},
    title: String,
    onClick: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .clickable {
                    onClick()
                }
                .height(48.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 14.sp,
            )
            Spacer(Modifier.weight(1f))
            ProvideTextStyle(
                value = TextStyle(
                    color = MixinAppTheme.colors.textSubtitle,
                    fontSize = 13.sp,
                ),
            ) {
                trailing()
            }
        }
    }
}

@Composable
fun YearMothSwitch() {
    var booleanValue by remember { mutableStateOf(false) }
    var number by remember { mutableStateOf("0") }
    Row(
        modifier = Modifier
            .wrapContentSize()
            .padding(8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = stringResource(id = R.string.Lately), color = MixinAppTheme.colors.textPrimary)
        Spacer(modifier = Modifier.width(4.dp))
        BasicTextField(
            modifier = Modifier
                .width(56.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MixinAppTheme.colors.red.copy(alpha = 0.4f))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            value = number,
            textStyle = TextStyle(
                fontSize = 14.sp,
                color = MixinAppTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
            ),
            onValueChange = {
                if (it.length <= 2 && it.all { char -> char.isDigit() }) {
                    number = it
                }
            },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            maxLines = 1,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Layout(
            modifier = Modifier
                .wrapContentWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(MixinAppTheme.colors.red.copy(alpha = 0.4f))
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .clickable {
                    booleanValue = !booleanValue
                },
            content = {
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MixinAppTheme.colors.red),
                )
                Text(
                    text = stringResource(id = R.string.yrs),
                    color = MixinAppTheme.colors.textPrimary,
                )
                Text(
                    text = stringResource(id = R.string.mos),
                    color = MixinAppTheme.colors.textPrimary,
                )
            },
        ) { measurables, constraints ->
            val placeables = measurables.map { measurable ->
                measurable.measure(constraints)
            }
            val firstPlaceable = placeables[0]
            val secondPlaceable = placeables[1]
            val thirdPlaceable = placeables[2]

            layout(secondPlaceable.width + thirdPlaceable.width + 8.dp.toPx().toInt(), secondPlaceable.height) {
                if (booleanValue) {
                    firstPlaceable.placeRelative(x = secondPlaceable.width + 8.dp.toPx().toInt(), y = 0)
                } else {
                    firstPlaceable.placeRelative(x = 0, y = 0)
                }
                secondPlaceable.placeRelative(x = 0, y = 0)
                thirdPlaceable.placeRelative(x = secondPlaceable.width + 8.dp.toPx().toInt(), y = 0)

                // LaunchedEffect(booleanValue) {
                //     animate(if (booleanValue) 0 else secondPlaceable.width) {
                //         firstPlaceable.translationX = it
                //     }
                // }
            }
        }
    }
}

@Composable
@Preview
fun SelectDatePagePreview() {
    SelectDatePage()
}
