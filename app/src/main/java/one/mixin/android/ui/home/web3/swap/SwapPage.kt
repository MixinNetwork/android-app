package one.mixin.android.ui.home.web3.swap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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
        Column(
            Modifier
                .padding(20.dp)
        ) {
            InputArea(token = inputToken.value, text = inputText)
            Box(modifier = Modifier.height(10.dp))
            InputArea(token = outputToken.value, text = outputText)
        }
    }
}

@Composable
private fun InputArea(
    token: Web3Token?,
    text: MutableState<String>,
) {
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MixinAppTheme.colors.backgroundGray)
    ) {
        Box(contentAlignment = Alignment.CenterEnd) {
            Text(text = token?.balance ?: "0")
        }
        InputContent(token, text)
    }
}

@Composable
private fun InputContent(
    token: Web3Token?,
    text: MutableState<String>,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(20.dp))
        GlideImage(
            data = token?.iconUrl ?: "",
            modifier =
            Modifier
                .size(50.dp)
                .clip(CircleShape),
            placeHolderPainter = painterResource(id = R.drawable.ic_avatar_place_holder),
        )
        Box(modifier = Modifier.width(10.dp))
        Text(
            text = token?.symbol ?: "",
            style = TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MixinAppTheme.colors.textPrimary,
            )
        )
        Box(modifier = Modifier.width(10.dp))
        Icon(
            painter = painterResource(id = R.drawable.ic_arrow_down_info),
            contentDescription = null,
            tint = MixinAppTheme.colors.icon,
        )
        Box(modifier = Modifier.width(20.dp))
        InputTextField(token = token, text = text)
        Box(modifier = Modifier.width(20.dp))
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