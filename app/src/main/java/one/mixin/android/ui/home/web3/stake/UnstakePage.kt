package one.mixin.android.ui.home.web3.stake

import PageScaffold
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import one.mixin.android.R
import one.mixin.android.api.response.solLamportToAmount
import one.mixin.android.api.response.web3.StakeAccount
import one.mixin.android.api.response.web3.StakeAccountActivation
import one.mixin.android.api.response.web3.StakeState
import one.mixin.android.api.response.web3.Validator
import one.mixin.android.api.response.web3.isActiveState
import one.mixin.android.api.response.web3.isDeactivatingState
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.formatPublicKey

@Composable
fun UnstakePage(
    validator: Validator,
    stakeAccount: StakeAccount,
    stakeActivation: StakeAccountActivation,
    isLoading: Boolean,
    onClick: () -> Unit,
    pop: () -> Unit,
) {
    val stakeState = stakeActivation.state
    PageScaffold(
        title = stringResource(id = R.string.Your_Stake),
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
                text = stringResource(id = R.string.unstake_hint),
                style =
                TextStyle(
                    fontWeight = FontWeight.W400,
                    color = MixinAppTheme.colors.textAssist,
                    fontSize = 16.sp,
                ),
            )
            Spacer(modifier = Modifier.height(20.dp))
            StakeInfo(validator, stakeAccount, stakeActivation)
            Spacer(modifier = Modifier.weight(1f))
            if (!stakeState.isDeactivatingState()) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onClick.invoke()
                    },
                    colors =
                    ButtonDefaults.outlinedButtonColors(
                        backgroundColor = MixinAppTheme.colors.accent,
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
                            text = stringResource(if (stakeState.isActiveState()) R.string.Unstake else R.string.Withdraw_Stake),
                            color = Color.White,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun StakeInfo(
    validator: Validator,
    stakeAccount: StakeAccount,
    stakeActivation: StakeAccountActivation,
) {
    val rentExempt = stakeAccount.account.data.parsed.info.meta.rentExemptReserve.toLongOrNull() ?: 0L
    val balance = stakeAccount.account.lamports
    val activeStake = balance - rentExempt
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
                model = validator.iconUrl,
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
            Text(
                text = stakeActivation.state,
                style =
                TextStyle(
                    fontWeight = FontWeight.W500,
                    color = if (stakeActivation.state == StakeState.active.name) {
                        MixinAppTheme.colors.green
                    } else if (stakeActivation.state == StakeState.inactive.name) {
                        MixinAppTheme.colors.red
                    } else {
                        MixinAppTheme.colors.textAssist
                    },
                    fontSize = 16.sp,
                ),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Divider(color = MixinAppTheme.colors.textAssist, thickness = 1.dp)
        Spacer(modifier = Modifier.height(12.dp))
        Item(stringResource(id = R.string.Address), stakeAccount.pubkey.formatPublicKey(20))
        Item(stringResource(id = R.string.Balance), "${balance.solLamportToAmount(5)} SOL")
        Item(stringResource(id = R.string.Rent_Reserve), "${rentExempt.solLamportToAmount(5)} SOL")
        Item(stringResource(id = R.string.Active_Stake), "${activeStake.solLamportToAmount(5)} SOL")
        Item(stringResource(id = R.string.Lookup_Until), "-")
        Item(stringResource(id = R.string.Last_Reward), "")
    }
}

@Preview
@Composable
private fun StakeInfoPreview() {
    val d = """{"data":[{"pubkey":"EvZfWbpG9HE7cJrq3o3d6gkZMcP8TLTisfVQMNLqGw4H","account":{"lamports":10009967,"owner":"Stake11111111111111111111111111111111111111","rentEpoch":18446744073709551615,"data":{"parsed":{"info":{"meta":{"authorized":{"staker":"5TDMKU3basuWC9sb9xAJgvn17KYFTLk9srPifmjZqJH9","withdrawer":"5TDMKU3basuWC9sb9xAJgvn17KYFTLk9srPifmjZqJH9"},"lockup":{"custodian":"11111111111111111111111111111111","epoch":0,"unixTimestamp":0},"rentExemptReserve":"2282880"},"stake":{"creditsObserved":96113126,"delegation":{"activationEpoch":"634","deactivationEpoch":"18446744073709551615","stake":"7726256","voter":"J2nUHEAgZFRyuJbFjdqPrAa9gyWDuc7hErtDQHPhsYRp","warmupCooldownRate":0.25}}},"type":"delegated"},"program":"stake","space":200},"executable":false}}]}"""
    val stakeAccount = Gson().fromJson(d, StakeAccount::class.java)
    StakeInfo(
        Validator("J2nUHEAgZFRyuJbFjdqPrAa9gyWDuc7hErtDQHPhsYRp", "Mixin Validator", "", "", "", "https://raw.githubusercontent.com/solana-labs/token-list/main/assets/mainnet/So11111111111111111111111111111111111111112/logo.png", "", 123123131231231, 9, 123124, 123123),
        stakeAccount,
        StakeAccountActivation("EvZfWbpG9HE7cJrq3o3d6gkZMcP8TLTisfVQMNLqGw4H", 0, 7717120, "activating"),
    )
}