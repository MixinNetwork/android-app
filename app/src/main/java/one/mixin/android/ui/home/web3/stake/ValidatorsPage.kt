package one.mixin.android.ui.home.web3.stake

import PageScaffold
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.api.response.web3.Validator
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.tip.wc.compose.Loading

@Composable
fun ValidatorsPage(
    searchText: String,
    isLoading: Boolean,
    validators: List<Validator>?,
    filterValidators: List<Validator>?,
    onClick: (Validator) -> Unit,
    onInputChanged: ((String) -> Unit)? = null,
    pop: () -> Unit,
) {
    MixinAppTheme {
        PageScaffold(
            title = stringResource(id = R.string.Select_Validator),
            verticalScrollable = true,
            pop = pop,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SearchInput(searchText, onInputChanged)
                Spacer(modifier = Modifier.height(20.dp))
                if (isLoading || validators.isNullOrEmpty()) {
                    Loading()
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (searchText.isNotBlank() && !filterValidators.isNullOrEmpty()) {
                            items(filterValidators.size) { i ->
                                ValidatorItem(filterValidators[i], onClick)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        } else {
                            items(validators.size) { i ->
                                ValidatorItem(validators[i], onClick)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ValidatorItem(
    validator: Validator,
    onClick: (Validator) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color = MixinAppTheme.colors.backgroundWindow)
            .clickable { onClick.invoke(validator) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoilImage(
            model = validator.iconUrl,
            modifier =
            Modifier
                .size(42.dp)
                .clip(CircleShape),
            placeholder = R.drawable.ic_avatar_place_holder,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Row {
                Text(
                    modifier = Modifier.fillMaxWidth(0.5f),
                    text = validator.name,
                    style =
                    TextStyle(
                        color = MixinAppTheme.colors.textPrimary,
                        fontSize = 16.sp,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${validator.commission}% ${stringResource(R.string.Fee)}",
                    style =
                    TextStyle(
                        color = MixinAppTheme.colors.textPrimary,
                        fontSize = 16.sp,
                    ),
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${validator.activatedStake}",
                style =
                TextStyle(
                    color = MixinAppTheme.colors.textAssist,
                    fontSize = 14.sp,
                ),
            )
        }
    }
}

@Composable
private fun SearchInput(
    text: String,
    onInputChanged: ((String) -> Unit)? = null,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocusedState = interactionSource.collectIsFocusedAsState()
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
        textStyle =
        TextStyle(
            fontSize = 16.sp,
            color = MixinAppTheme.colors.textPrimary,
            textAlign = TextAlign.Start,
        ),
        cursorBrush = SolidColor(MixinAppTheme.colors.accent),
    ) { innerTextField ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, color = MixinAppTheme.colors.backgroundDark, RoundedCornerShape(12.dp))
                .padding(16.dp, 10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_search),
                    contentDescription = null,
                    tint = MixinAppTheme.colors.textAssist,
                )
                Spacer(modifier = Modifier.width(10.dp))
                if (text.isEmpty() || !isFocusedState.value) {
                    Text(
                        text = stringResource(R.string.Search),
                        style =
                        TextStyle(
                            color = MixinAppTheme.colors.textAssist,
                            fontSize = 16.sp,
                        ),
                    )
                } else {
                    innerTextField()
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewValidatorItem() {
    val validator = Validator("J2nUHEAgZFRyuJbFjdqPrAa9gyWDuc7hErtDQHPhsYRp", "Mixin Validator", "", "", "", "https://raw.githubusercontent.com/solana-labs/token-list/main/assets/mainnet/So11111111111111111111111111111111111111112/logo.png", "", 123123131231231, 9, 123124, 123123)
    ValidatorItem(validator) { }
}

@Preview
@Composable
fun PreviewSearchInput() {
    SearchInput("")
}