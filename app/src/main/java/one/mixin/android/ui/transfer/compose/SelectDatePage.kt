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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.MixinTopAppBar
import one.mixin.android.compose.theme.MixinAppTheme

@Composable
fun SelectDatePage(
    onExit: () -> Unit,
    onResult: (Int?) -> Unit,
) {
    var dateSelect by remember {
        mutableStateOf(false)
    }

    var unit by remember {
        mutableStateOf(0)
    }

    var localText by remember {
        mutableStateOf("1")
    }
    MixinAppTheme {
        Scaffold(
            backgroundColor = MixinAppTheme.colors.background,
            topBar = {
                MixinTopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onExit) {
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
                            onClick = {
                                if (dateSelect) {
                                    val date = localText.toIntOrNull()
                                    if (date == null) {
                                        onResult.invoke(null)
                                    } else {
                                        onResult.invoke(
                                            if (unit == 1) {
                                                date
                                            } else {
                                                date * 12
                                            },
                                        )
                                    }
                                } else {
                                    onResult.invoke(null)
                                }
                            },
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
                modifier =
                    Modifier
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
                SelectItem(stringResource(id = R.string.designated_time_period), dateSelect) {
                    dateSelect = true
                }
                AnimatedVisibility(
                    visible = dateSelect,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { 0 }),
                ) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        YearMothSwitch { text, index ->
                            localText = text ?: ""
                            unit = index
                        }
                    }
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
            modifier =
                Modifier
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
                value =
                    TextStyle(
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
fun YearMothSwitch(onTextChange: (String?, Int) -> Unit) {
    var number by remember { mutableStateOf("1") }
    var index by remember { mutableStateOf(0) }
    DisposableEffect(Unit) {
        onDispose {
            onTextChange(number, index)
        }
    }
    Row(
        modifier =
            Modifier
                .wrapContentSize()
                .padding(8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = stringResource(id = R.string.recent), color = MixinAppTheme.colors.textPrimary)
        Spacer(modifier = Modifier.width(12.dp))
        BasicTextField(
            cursorBrush = SolidColor(MixinAppTheme.colors.accent),
            modifier =
                Modifier
                    .width(56.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MixinAppTheme.colors.shadow)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            value = number,
            textStyle =
                TextStyle(
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.textPrimary,
                    textAlign = TextAlign.Center,
                ),
            onValueChange = {
                if (it.length <= 2 && it.all { char -> char.isDigit() }) {
                    number = it
                    onTextChange(number, index)
                }
            },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            maxLines = 1,
        )
        Spacer(modifier = Modifier.width(4.dp))
        SegmentedControl(
            items = listOf(stringResource(id = R.string.Year), stringResource(id = R.string.Month)),
            defaultSelectedItemIndex = index,
            onItemSelection = {
                index = it
                onTextChange(number, index)
            },
        )
    }
}

@Composable
fun SegmentedControl(
    items: List<String>,
    defaultSelectedItemIndex: Int = 0,
    onItemSelection: (selectedItemIndex: Int) -> Unit,
) {
    val selectedIndex = remember { mutableStateOf(defaultSelectedItemIndex) }

    Row(
        modifier =
            Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(MixinAppTheme.colors.shadow)
                .wrapContentHeight()
                .padding(2.dp),
    ) {
        items.forEachIndexed { index, item ->
            val background =
                if (index == selectedIndex.value) {
                    MixinAppTheme.colors.background
                } else {
                    Color.Transparent
                }
            Text(
                text = item,
                color = MixinAppTheme.colors.textPrimary,
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(background)
                        .padding(horizontal = 20.dp, vertical = 3.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                selectedIndex.value = index
                                onItemSelection.invoke(selectedIndex.value)
                            },
                        ),
            )
        }
    }
}

@Composable
@Preview
fun SelectDatePagePreview() {
    SelectDatePage(onExit = {}, onResult = {})
}
