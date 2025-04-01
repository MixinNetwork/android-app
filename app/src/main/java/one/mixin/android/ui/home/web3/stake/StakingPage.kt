package one.mixin.android.ui.home.web3.stake

import PageScaffold
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import one.mixin.android.R
import one.mixin.android.api.response.web3.StakeAccount
import one.mixin.android.api.response.web3.StakeAccountActivation
import one.mixin.android.api.response.web3.Validator
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun StakingPage(
    stakeAccounts: List<StakeAccount>,
    activations: Map<String, StakeAccountActivation?>,
    validators: Map<String, Validator?>,
    onClick: (StakeAccount, Validator?, StakeAccountActivation?) -> Unit,
    onAdd: () -> Unit,
    pop: () -> Unit,
) {
    MixinAppTheme {
        PageScaffold(
            title = stringResource(id = R.string.Your_Stake),
            verticalScrollable = true,
            actions = {
                IconButton(onClick = { onAdd.invoke() }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add_black_24dp),
                        contentDescription = null,
                        tint = MixinAppTheme.colors.icon,
                    )
                }
            },
            pop = pop,
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                items(stakeAccounts.size) { i ->
                    val sa = stakeAccounts[i]
                    StakeAccountItem(
                        stakeAccount = sa,
                        activations[sa.pubkey],
                        validators[sa.account.data.parsed.info.stake.delegation.voter],
                        onClick
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun StakeAccountItem(
    stakeAccount: StakeAccount,
    activation: StakeAccountActivation?,
    validator: Validator?,
    onClick: (StakeAccount, Validator?, StakeAccountActivation?) -> Unit,
) {
    Row(
      modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(color = MixinAppTheme.colors.backgroundWindow)
          .clickable { onClick.invoke(stakeAccount, validator, activation) }
          .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoilImage(
            model = validator?.iconUrl ?: "",
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
                    text = validator?.name ?: "",
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
                    text = "${activation?.let { calcStakeAmount(it) } ?: "0"} SOL",
                    style =
                    TextStyle(
                        color = MixinAppTheme.colors.textPrimary,
                        fontSize = 14.sp,
                    ),
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = activation?.state ?: "",
                style =
                TextStyle(
                    color = MixinAppTheme.colors.accent,
                    fontSize = 14.sp,
                ),
            )
        }
    }
}

private fun calcStakeAmount(activation: StakeAccountActivation): String {
    return try {
        BigDecimal(activation.active + activation.inactive).divide(BigDecimal.TEN.pow(9)).setScale(9, RoundingMode.CEILING).stripTrailingZeros().toPlainString()
    } catch (e: Exception) {
        "0"
    }
}

@Preview
@Composable
private fun StakeAccountItemPreview() {
    val d = """{"data":[{"pubkey":"EvZfWbpG9HE7cJrq3o3d6gkZMcP8TLTisfVQMNLqGw4H","account":{"lamports":10009967,"owner":"Stake11111111111111111111111111111111111111","rentEpoch":18446744073709551615,"data":{"parsed":{"info":{"meta":{"authorized":{"staker":"5TDMKU3basuWC9sb9xAJgvn17KYFTLk9srPifmjZqJH9","withdrawer":"5TDMKU3basuWC9sb9xAJgvn17KYFTLk9srPifmjZqJH9"},"lockup":{"custodian":"11111111111111111111111111111111","epoch":0,"unixTimestamp":0},"rentExemptReserve":"2282880"},"stake":{"creditsObserved":96113126,"delegation":{"activationEpoch":"634","deactivationEpoch":"18446744073709551615","stake":"7726256","voter":"J2nUHEAgZFRyuJbFjdqPrAa9gyWDuc7hErtDQHPhsYRp","warmupCooldownRate":0.25}}},"type":"delegated"},"program":"stake","space":200},"executable":false}}]}"""
    val stakeAccount = Gson().fromJson(d, StakeAccount::class.java)
    val validator = Validator("J2nUHEAgZFRyuJbFjdqPrAa9gyWDuc7hErtDQHPhsYRp", "Mixin Validator", "", "", "", "https://raw.githubusercontent.com/solana-labs/token-list/main/assets/mainnet/So11111111111111111111111111111111111111112/logo.png", "", 123123131231231, 9, 123124, 123123)
    val activation = StakeAccountActivation("EvZfWbpG9HE7cJrq3o3d6gkZMcP8TLTisfVQMNLqGw4H", 0, 7717120, "activating")
    StakeAccountItem(stakeAccount = stakeAccount, activation, validator) {_,_,_ ->}
}