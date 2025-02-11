package one.mixin.android.ui.address.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.request.ImageRequest
import coil3.request.transformations
import one.mixin.android.R
import one.mixin.android.api.response.Web3Token
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.util.getChainNetwork
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.widget.CoilRoundedHexagonTransformation

@Composable
fun TokenInfoHeader(
    token: TokenItem?,
    web3Token: Web3Token?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (token?.collectionHash != null) {
            CoilImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(token.iconUrl)
                    .transformations(CoilRoundedHexagonTransformation()).build(),
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(4.dp)),
                placeholder = R.drawable.ic_inscription_icon,
            )
        } else {
            CoilImage(
                model = token?.iconUrl ?: web3Token?.iconUrl ?: "",
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape),
                placeholder = R.drawable.ic_avatar_place_holder,
            )
        }

        Text(
            text = token?.name ?: web3Token?.name ?: "",
            color = MixinAppTheme.colors.textPrimary,
            fontSize = 14.sp,
            modifier = Modifier
                .padding(start = 4.dp)
                .wrapContentWidth()
                .widthIn(max = 80.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        token?.let { token ->
            val networkName =
                getChainNetwork(token.assetId, token.chainId, token.chainId)
            if (!networkName.isNullOrEmpty()) {
                Text(
                    text = networkName,
                    color = MixinAppTheme.colors.textAssist,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .background(
                            color = MixinAppTheme.colors.backgroundWindow,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }

        web3Token?.let {
            Text(
                text = it.chainName,
                color = MixinAppTheme.colors.textAssist,
                fontSize = 12.sp,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .background(
                        color = MixinAppTheme.colors.backgroundWindow,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "Bal: ${token?.balance ?: web3Token?.balance} ${token?.symbol ?: web3Token?.symbol}",
            fontSize = 14.sp,
            color = MixinAppTheme.colors.textAssist
        )
    }
}