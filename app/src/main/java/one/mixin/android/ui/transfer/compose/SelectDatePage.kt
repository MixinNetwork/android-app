package one.mixin.android.ui.transfer.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.ui.setting.ui.compose.MixinTopAppBar
import one.mixin.android.ui.setting.ui.theme.MixinAppTheme
import kotlin.math.max

@Composable
fun SelectDatePage(onClick: () -> Unit) {
    var dateSelect by remember {
        mutableStateOf(false)
    }
    val dateValue by remember {
        mutableStateOf(6)
    }

    Scaffold(
        backgroundColor = MixinAppTheme.colors.background,
        topBar = {
            MixinTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = null,
                            tint = MixinAppTheme.colors.icon,
                        )
                    }
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
                    TextButton(
                        onClick = onClick,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
                    ) {
                        Text(
                            stringResource(id = R.string.Save),
                            color = MixinAppTheme.colors.accent,
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
                .padding(16.dp)
                .animateContentSize()
                .clip(RoundedCornerShape(8.dp))
                .background(MixinAppTheme.colors.backgroundGray),
        ) {
            SelectItem(stringResource(id = R.string.all_time), !dateSelect) {
                dateSelect = false
            }
            SelectItem(stringResource(id = R.string.Specified_time_period), dateSelect) {
                dateSelect = true
            }
            AnimatedVisibility(
                visible = dateSelect,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { 0 }),
            ) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    YearMothSwitch()
                }
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
                .background(MixinAppTheme.colors.shadow)
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
                .background(MixinAppTheme.colors.shadow)
                .padding(2.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    onClick =
                    {
                        booleanValue = !booleanValue
                    },
                    indication = null,
                ),
            content = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MixinAppTheme.colors.shadow),
                )
                Text(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    text = stringResource(id = R.string.yrs),
                    textAlign = TextAlign.Center,
                    color = MixinAppTheme.colors.textPrimary,
                )
                Text(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    text = stringResource(id = R.string.mos),
                    textAlign = TextAlign.Center,
                    color = MixinAppTheme.colors.textPrimary,
                )
            },
        ) { measurables, constraints ->
            val secondPlaceable: Placeable = measurables[1].measure(constraints)
            val thirdPlaceable: Placeable = measurables[2].measure(constraints)
            val width = max(secondPlaceable.width, thirdPlaceable.width)
            val height = max(secondPlaceable.height, thirdPlaceable.height)
            val itemConstraints = Constraints.fixed(
                width,
                height,
            )
            val firstPlaceable = measurables[0].measure(itemConstraints)
            val offset = (width - secondPlaceable.width) / 2

            layout(width * 2 + 8.dp.toPx().toInt(), height) {
                if (booleanValue) {
                    firstPlaceable.placeRelative(x = width + 8.dp.toPx().toInt(), y = 0)
                } else {
                    firstPlaceable.placeRelative(x = 0, y = 0)
                }
                if (offset > 0) {
                    secondPlaceable.placeRelative(x = offset, y = 0)
                } else {
                    secondPlaceable.placeRelative(x = 0, y = 0)
                }
                if (offset < 0) {
                    thirdPlaceable.placeRelative(x = width + 8.dp.toPx().toInt() - offset, y = 0)
                } else {
                    thirdPlaceable.placeRelative(x = width + 8.dp.toPx().toInt(), y = 0)
                }
            }
        }
    }
}

@Composable
@Preview
fun SelectDatePagePreview() {
    SelectDatePage(onClick = {
    })
}
