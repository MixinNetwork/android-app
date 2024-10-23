package one.mixin.android.ui.landing.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme

@Composable
fun CreateAccountPage(
    toMobile: () -> Unit,
    toMnemonic: () -> Unit,
    onConvenienceClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onTermsOfServiceClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit
) {
    MixinAppTheme {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Spacer(modifier = Modifier.height(10.dp))
            CreateItem(R.drawable.ic_create_mobile, R.string.Mobile_Phone, R.string.create_introduction, R.string.Convenience, toMobile) { onConvenienceClick.invoke() }
            Spacer(modifier = Modifier.height(10.dp))
            CreateItem(R.drawable.ic_create_mnemonic_phrase, R.string.Mnemonic_Phrase, R.string.create_introduction, R.string.Privacy, toMnemonic) { onPrivacyClick.invoke() }
            Spacer(modifier = Modifier.height(10.dp))
            val privacyPolicyText = stringResource(R.string.Privacy_Policy)
            val termsOfServiceText = stringResource(R.string.Terms_of_Service)
            val landingIntroduction = stringResource(R.string.landing_introduction, privacyPolicyText, termsOfServiceText)
            HighlightedTextWithClick(
                landingIntroduction,
                modifier = Modifier.align(Alignment.CenterHorizontally), privacyPolicyText, termsOfServiceText, onTextClick = { str ->
                    when (str) {
                        privacyPolicyText -> {
                            onPrivacyPolicyClick.invoke()
                        }

                        termsOfServiceText -> {
                            onTermsOfServiceClick.invoke()
                        }
                    }
                })
            Spacer(modifier = Modifier.weight(1f))
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                onClick = {
                    toMobile.invoke()
                },
                colors =
                ButtonDefaults.outlinedButtonColors(
                    backgroundColor = MixinAppTheme.colors.backgroundWindow
                ),
                shape = RoundedCornerShape(32.dp),
                elevation =
                ButtonDefaults.elevation(
                    pressedElevation = 0.dp,
                    defaultElevation = 0.dp,
                    hoveredElevation = 0.dp,
                    focusedElevation = 0.dp,
                ),
            ) {

                Text(
                    text = stringResource(R.string.landing_have_account),
                    color = MixinAppTheme.colors.textBlue
                )
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun CreateItem(@DrawableRes iconId: Int, @StringRes titleId: Int, @StringRes subTitleId: Int, @StringRes highlightTextId: Int, onClick: () -> Unit, highlightClick: (String) -> Unit) {
    ConstraintLayout(
        modifier = Modifier
            .clip(
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .fillMaxWidth()
            .background(MixinAppTheme.colors.backgroundWindow)
            .padding(vertical = 20.dp)
    ) {
        val (starIcon, title, subtitle, endIcon) = createRefs()
        Icon(
            modifier = Modifier.constrainAs(starIcon) {
                top.linkTo(parent.top)
                start.linkTo(parent.start, 20.dp)
            },
            painter = painterResource(iconId),
            contentDescription = null,
            tint = Color.Unspecified
        )
        Icon(
            modifier = Modifier.constrainAs(endIcon) {
                top.linkTo(parent.top)
                end.linkTo(parent.end, 20.dp)
                bottom.linkTo(parent.bottom)
            },
            painter = painterResource(R.drawable.ic_arrow_gray_right),
            contentDescription = null,
            tint = Color.Unspecified
        )
        Text(
            stringResource(titleId), modifier = Modifier.constrainAs(title) {
                top.linkTo(starIcon.top)
                start.linkTo(starIcon.end, 16.dp)
            },
            lineHeight = 16.sp,
            fontWeight = FontWeight.W600, color = MixinAppTheme.colors.textPrimary, fontSize = 16.sp
        )
        HighlightedTextWithClick(
            stringResource(subTitleId, stringResource(highlightTextId)),
            modifier = Modifier.constrainAs(subtitle) {
                top.linkTo(title.bottom, 8.dp)
                linkTo(title.start, endIcon.start, endMargin = 20.dp, bias = 0f)
                width = Dimension.fillToConstraints
            }, stringResource(highlightTextId), onTextClick = highlightClick
        )
    }
}

@Preview
@Composable
fun CreateAccountPagePreview() {
    CreateAccountPage({},{}, {}, {}, {}, {})
}