package one.mixin.android.ui.home.reminder

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.landing.components.HighlightedTextWithClick

@Composable
fun ReminderPage(
    @DrawableRes contentImage: Int,
    @StringRes title: Int,
    @StringRes actionStr: Int,
    @StringRes dismissStr: Int? = R.string.Not_Now,
    action: () -> Unit,
    dismiss: (() -> Unit)?,
    contentSlot: @Composable () -> Unit,
    extraContent: (@Composable () -> Unit)? = null,
    stickyFooter: Boolean = false,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .clip(RoundedCornerShape(topEnd = 12.dp, topStart = 12.dp))
            .background(MixinAppTheme.colors.primary)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MixinAppTheme.colors.bgGradientStart,
                            MixinAppTheme.colors.bgGradientEnd
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(0f, Float.POSITIVE_INFINITY)
                    )
                )
                .padding(horizontal = 22.dp)
                .padding(top = 30.dp)
        ) {
            Image(
                painter = painterResource(id = contentImage),
                contentDescription = null,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
        if (stickyFooter) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ReminderContent(
                            title = title,
                            contentSlot = contentSlot,
                            extraContent = extraContent,
                        )
                    }
                }
                ReminderActions(
                    actionStr = actionStr,
                    dismissStr = dismissStr,
                    action = action,
                    dismiss = dismiss,
                )
            }
        } else {
            Column(
                modifier = Modifier.padding(horizontal = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ReminderContent(
                    title = title,
                    contentSlot = contentSlot,
                    extraContent = extraContent,
                )
                ReminderActions(
                    actionStr = actionStr,
                    dismissStr = dismissStr,
                    action = action,
                    dismiss = dismiss,
                )
            }
        }
    }
}

@Composable
private fun ReminderContent(
    @StringRes title: Int,
    contentSlot: @Composable () -> Unit,
    extraContent: (@Composable () -> Unit)?,
) {
    Spacer(modifier = Modifier.height(42.dp))
    Text(
        stringResource(title), color = MixinAppTheme.colors.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.W600
    )
    Spacer(modifier = Modifier.height(10.dp))
    contentSlot()
    if (extraContent != null) {
        Spacer(modifier = Modifier.height(20.dp))
        extraContent()
    }
    Spacer(modifier = Modifier.height(20.dp))
}

@Composable
private fun ReminderActions(
    @StringRes actionStr: Int,
    @StringRes dismissStr: Int?,
    action: () -> Unit,
    dismiss: (() -> Unit)?,
) {
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        onClick = action,
        colors = ButtonDefaults.outlinedButtonColors(
            backgroundColor = MixinAppTheme.colors.accent
        ),
        shape = RoundedCornerShape(32.dp),
        elevation = ButtonDefaults.elevation(
            pressedElevation = 0.dp,
            defaultElevation = 0.dp,
            hoveredElevation = 0.dp,
            focusedElevation = 0.dp,
        ),
    ) {
        Text(text = stringResource(actionStr), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.W400)
    }
    if (dismissStr != null && dismiss != null) {
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            modifier = Modifier
                .padding(8.dp)
                .clickable { dismiss.invoke() },
            text = stringResource(dismissStr),
            color = MixinAppTheme.colors.accent,
            fontSize = 14.sp,
            fontWeight = FontWeight.W400
        )
        Spacer(modifier = Modifier.height(20.dp))
    } else {
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun ReminderItem(title: String, description: String, checked: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                tint = Color.Unspecified,
                contentDescription = null,
                painter = painterResource(if (checked) R.drawable.ic_check_circle_filled else R.drawable.ic_check_circle_empty),
                modifier = Modifier.padding(end = 10.dp)
            )
            Text(title, fontSize = 16.sp, color = MixinAppTheme.colors.textMinor)
        }
        Text(description, fontSize = 15.sp, color = MixinAppTheme.colors.textAssist)
    }
}

@Preview(showBackground = true)
@Composable
private fun ReminderPagePreview() {
    MixinAppTheme {
        ReminderPage(
            contentImage = R.drawable.bg_recovery_kit,
            title = R.string.Recovery_Kit,
            actionStr = R.string.Continue,
            dismissStr = R.string.Not_Now,
            action = {},
            dismiss = {},
            contentSlot = {
                HighlightedTextWithClick(
                    fullText = stringResource(R.string.Recovery_Kit_Alert),
                    modifier = Modifier.fillMaxWidth(),
                    stringResource(R.string.More_Information),
                    onTextClick = {}
                )
            },
            extraContent = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ReminderItem(stringResource(R.string.Mobile_Number), stringResource(R.string.Added), checked = true)
                    ReminderItem(stringResource(R.string.Mnemonic_Phrase), stringResource(R.string.Backup), checked = false)
                    ReminderItem(stringResource(R.string.Recovery_Contact), stringResource(R.string.Not_Added), checked = false)
                }
            }
        )
    }
}
