package one.mixin.android.ui.home.web3.stake

import PageScaffold
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.api.response.web3.Validator
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.home.web3.swap.checkBalance

@Composable
fun StakePage(
    validator: Validator,
    amountText: String,
    balance: String,
    isLoading: Boolean,
    onInputChanged: ((String) -> Unit)? = null,
    onChooseValidator: () -> Unit,
    onMax:() -> Unit,
    onStake: () -> Unit,
    pop: () -> Unit,
) {
    PageScaffold(
        title = stringResource(id = R.string.Start_Staking),
        verticalScrollable = true,
        pop = pop,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .align(Alignment.CenterHorizontally),
                text = stringResource(id = R.string.stake_choose_amount),
                style =
                TextStyle(
                    fontWeight = FontWeight.W400,
                    color = MixinAppTheme.colors.textSubtitle,
                    fontSize = 16.sp,
                ),
            )
            Spacer(modifier = Modifier.height(20.dp))
            Input(amountText, onInputChanged)
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
            ) {
                Text(
                    text = stringResource(id = R.string.Validator),
                    style =
                    TextStyle(
                        fontWeight = FontWeight.W400,
                        color = MixinAppTheme.colors.textPrimary,
                        fontSize = 16.sp,
                    ),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(id = R.string.Choose),
                    style =
                    TextStyle(
                        fontWeight = FontWeight.W400,
                        color = MixinAppTheme.colors.accent,
                        fontSize = 16.sp,
                    ),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            ValidatorInfo(validator = validator)
            Spacer(modifier = Modifier.weight(1f))
            val checkBalance = checkBalance(amountText, balance)
            val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
            val focusManager = LocalFocusManager.current
            if (amountText.isNotEmpty()) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        onStake.invoke()
                    },
                    colors =
                    ButtonDefaults.outlinedButtonColors(
                        backgroundColor = if (checkBalance != true) MixinAppTheme.colors.backgroundGray else MixinAppTheme.colors.accent,
                    ),
                    shape = RoundedCornerShape(32.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    elevation =
                    ButtonDefaults.elevation(
                        pressedElevation = 0.dp,
                        defaultElevation = 0.dp,
                        hoveredElevation = 0.dp,
                        focusedElevation = 0.dp,
                    ),
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                        )
                    } else {
                        Text(
                            text = if (checkBalance == false) "SOL ${stringResource(R.string.insufficient_balance)}" else stringResource(R.string.Confirm),
                            color = if (checkBalance != true) MixinAppTheme.colors.textSubtitle else Color.White,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun Input(
    text: String,
    onInputChanged: ((String) -> Unit)? = null,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val interactionSource = remember { MutableInteractionSource() }
    BasicTextField(
        value = text,
        onValueChange = {
            onInputChanged?.invoke(it)
        },
        maxLines = 1,
        modifier =
        Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged {
                if (it.isFocused) {
                    keyboardController?.show()
                }
            },
        interactionSource = interactionSource,
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
        textStyle =
        TextStyle(
            fontSize = 18.sp,
            color = MixinAppTheme.colors.textPrimary,
            textAlign = TextAlign.Start,
        ),
        cursorBrush = SolidColor(MixinAppTheme.colors.accent),
    ) { innerTextField ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, color = MixinAppTheme.colors.backgroundDark, RoundedCornerShape(12.dp))
                .padding(16.dp, 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            innerTextField()
            Spacer(modifier = Modifier.width(10.dp))
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier.wrapContentWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "SOL",
                    style =
                    TextStyle(
                        fontWeight = FontWeight.W400,
                        color = MixinAppTheme.colors.textSubtitle,
                        fontSize = 16.sp,
                    ),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .wrapContentWidth()
                        .wrapContentHeight()
                        .border(1.dp, color = MixinAppTheme.colors.backgroundGray, RoundedCornerShape(16.dp))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) {

                        }
                        .padding(8.dp, 4.dp),
                ) {
                    Text(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        text = stringResource(id = R.string.Max),
                        style =
                        TextStyle(
                            fontWeight = FontWeight.W400,
                            color = MixinAppTheme.colors.textPrimary,
                            fontSize = 16.sp,
                        ),
                    )
                }
            }

        }
    }
}

@Composable
private fun ValidatorInfo(
    validator: Validator,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color = MixinAppTheme.colors.backgroundWindow)
            .padding(12.dp),
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoilImage(
                model = validator.icon ?: "",
                modifier =
                Modifier
                    .size(28.dp)
                    .clip(CircleShape),
                placeholder = R.drawable.ic_avatar_place_holder,
            )
            Spacer(modifier = Modifier.width(20.dp))
            Text(
                text = validator.name,
                style =
                TextStyle(
                    fontWeight = FontWeight.W500,
                    color = MixinAppTheme.colors.textPrimary,
                    fontSize = 18.sp,
                ),
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(id = R.drawable.ic_link),
                contentDescription = null,
                tint = MixinAppTheme.colors.icon,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Divider(color = MixinAppTheme.colors.textSubtitle, thickness = 1.dp)
        Spacer(modifier = Modifier.height(12.dp))
        Item(stringResource(id = R.string.Estimated_APY), validator.estimatedApy)
        Item(stringResource(id = R.string.Commission), validator.commission)
        Item(stringResource(id = R.string.Total_Stake), validator.totalStake)
    }

}

@Composable
private fun Item(
    key: String,
    value: String,
) {
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(16.dp, 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = key,
            color = MixinAppTheme.colors.textSubtitle,
            fontSize = 14.sp,
            fontWeight = FontWeight.W400
        )
        Box(modifier = Modifier.weight(1f))
        Text(
            modifier = Modifier.fillMaxWidth(0.8f),
            textAlign = TextAlign.End,
            text = value,
            color = MixinAppTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 14.sp,
        )
    }
}

@Preview
@Composable
private fun InputPreview() {
    Input(text = "123")
}

@Preview
@Composable
private fun ValidatorInfoPreview() {
    ValidatorInfo(Validator("Mixin Validator", "https://raw.githubusercontent.com/solana-labs/token-list/main/assets/mainnet/So11111111111111111111111111111111111111112/logo.png", "6.9%", "9%", "412,456.1234", "J2nUHEAgZFRyuJbFjdqPrAa9gyWDuc7hErtDQHPhsYRp"))
}