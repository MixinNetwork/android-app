package one.mixin.android.ui.wallet.alert.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme

@Composable
fun AlertSelectItem(
    selected: Boolean,
    iconId: Int,
    titleId: Int,
    subTitleId: Int,
    onClick: () -> Unit
) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(color = MixinAppTheme.colors.backgroundWindow)
            .padding(vertical = 12.dp, horizontal = 20.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
    )
    {
        val (starIcon, endIcon, title, subtitle) = createRefs()

        Image(
            painter = painterResource(id = iconId),
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .constrainAs(starIcon) {
                    top.linkTo(parent.top, 5.dp)
                    start.linkTo(parent.start)
                }
        )

        Image(
            painter = painterResource(id = R.drawable.ic_alert_selected),
            contentDescription = null,
            modifier = Modifier
                .alpha(if (selected) 1f else 0f)
                .size(24.dp)
                .constrainAs(endIcon) {
                    top.linkTo(starIcon.top)
                    bottom.linkTo(starIcon.bottom)
                    end.linkTo(parent.end)
                }
        )
        Text(
            modifier = Modifier
                .constrainAs(title) {
                    top.linkTo(parent.top)
                    end.linkTo(parent.end)
                    linkTo(starIcon.end, endIcon.start, startMargin = 12.dp, endMargin = 12.dp, bias = 0f)
                    width = Dimension.fillToConstraints
                },
            text = stringResource(id = titleId),
            style =
            TextStyle(
                color = MixinAppTheme.colors.textMinor,
                fontSize = 16.sp,
            ),
        )

        Text(
            modifier = Modifier
                .constrainAs(subtitle) {
                    top.linkTo(title.bottom, 6.dp)
                    linkTo(title.start, endIcon.start, endMargin = 12.dp, bias = 0f)
                    width = Dimension.fillToConstraints
                },
            text = stringResource(id = subTitleId),
            style =
            TextStyle(
                color = MixinAppTheme.colors.textAssist,
                fontSize = 13.sp,
            ),
        )
    }
}

