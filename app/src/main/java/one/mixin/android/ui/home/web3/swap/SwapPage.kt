package one.mixin.android.ui.home.web3.swap

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import one.mixin.android.R
import one.mixin.android.api.response.Web3Token
import one.mixin.android.compose.GlideImage
import one.mixin.android.compose.MixinTopAppBar
import one.mixin.android.compose.theme.MixinAppTheme

@Composable
fun SwapPage(
    token: Web3Token,
    pop: () -> Unit,
) {
    SwapPageScaffold(
        title = stringResource(id = R.string.Swap),
        verticalScrollable = true,
        pop = pop,
    ) {
        val viewModel = hiltViewModel<SwapViewModel>()
        val inputToken = rememberSaveable {
            mutableStateOf<Web3Token?>(token)
        }
        val outputToken = rememberSaveable {
            mutableStateOf<Web3Token?>(token)
        }
        val inputText =
            rememberSaveable {
                mutableStateOf("0")
            }
        val outputText =
            rememberSaveable {
                mutableStateOf("0")
            }
        var isReverse by remember { mutableStateOf(false) }
        val rotation by animateFloatAsState(if (isReverse) 180f else 0f, label = "rotation")

        SwapLayout(
            center = {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(40.dp)
                        .clip(CircleShape)
                        .border(width = 6.dp, color = MixinAppTheme.colors.background, shape = CircleShape)
                        .background(MixinAppTheme.colors.backgroundGray)
                        .clickable {
                            isReverse = !isReverse
                        }
                        .rotate(rotation),
                    contentAlignment = Alignment.Center

                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_switch),
                        contentDescription = null,
                        tint = MixinAppTheme.colors.textPrimary,
                    )
                }
            },
            content = {
                InputArea(token = inputToken.value, text = inputText, title = stringResource(id = if (isReverse) R.string.To else R.string.From))
                Spacer(modifier = Modifier.height(6.dp))
                InputArea(token = outputToken.value, text = outputText, title = stringResource(id = if (isReverse) R.string.From else R.string.To))
            },
            reverse = isReverse
        )
    }
}

@Composable
fun InputArea(
    token: Web3Token?,
    text: MutableState<String>,
    title: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(MixinAppTheme.colors.backgroundGray)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, color = MixinAppTheme.colors.textMinor)
                Spacer(modifier = Modifier.width(4.dp))
                GlideImage(
                    data = token?.iconUrl ?: "",
                    modifier =
                    Modifier
                        .size(14.dp)
                        .clip(CircleShape),
                    placeHolderPainter = painterResource(id = R.drawable.ic_avatar_place_holder),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = token?.chainName ?: "", color = MixinAppTheme.colors.textMinor)
            }
            Text(
                text = token?.balance ?: "0", modifier = Modifier.fillMaxWidth(),
                style = TextStyle(
                    color = MixinAppTheme.colors.textMinor,
                    textAlign = TextAlign.End
                )
            )
        }
        InputContent(
            token, text
        )
    }
}

@Composable
private fun InputContent(
    token: Web3Token?,
    text: MutableState<String>,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlideImage(
            data = token?.iconUrl ?: "",
            modifier =
            Modifier
                .size(32.dp)
                .clip(CircleShape),
            placeHolderPainter = painterResource(id = R.drawable.ic_avatar_place_holder),
        )
        Box(modifier = Modifier.width(10.dp))
        Text(
            text = token?.symbol ?: "",
            style = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = MixinAppTheme.colors.textPrimary,
            )
        )
        Box(modifier = Modifier.width(4.dp))
        Icon(
            painter = painterResource(id = R.drawable.ic_arrow_down_info),
            contentDescription = null,
            tint = MixinAppTheme.colors.icon,
        )
        Box(modifier = Modifier.width(10.dp))
        InputTextField(token = token, text = text)
    }
}

@Composable
fun SwapPageScaffold(
    title: String,
    verticalScrollable: Boolean = true,
    pop: () -> Unit,
    body: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        backgroundColor = MixinAppTheme.colors.background,
        topBar = {
            MixinTopAppBar(
                title = {
                    Text(title)
                },
                navigationIcon = {
                    IconButton(onClick = { pop() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = null,
                            tint = MixinAppTheme.colors.icon,
                        )
                    }
                },
            )
        },
    ) {
        Column(
            Modifier
                .padding(it)
                .apply {
                    if (verticalScrollable) {
                        verticalScroll(rememberScrollState())
                    }
                },
        ) {
            body()
        }
    }
}

@Composable
fun SwapLayout(
    content: @Composable ColumnScope.() -> Unit,
    center: @Composable BoxScope.() -> Unit,
    reverse: Boolean
) {
    Box(
        modifier = Modifier
            .wrapContentHeight()
            .wrapContentWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = if (reverse) Arrangement.Bottom else Arrangement.Top
        ) {
            content()
        }
        center()
    }
}
