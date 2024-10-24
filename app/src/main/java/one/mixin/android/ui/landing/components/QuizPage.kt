package one.mixin.android.ui.landing.components

import PageScaffold
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme

@Composable
fun QuizPage(next: () -> Unit) {
    var selectedOption by remember { mutableStateOf(-1) }
    PageScaffold(
        title = "",
        verticalScrollable = false,
        actions = {
            IconButton(onClick = {}) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_support),
                    contentDescription = null,
                    tint = MixinAppTheme.colors.icon,
                )
            }
        },
        pop = null,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 20.dp)) {
            Spacer(modifier = Modifier.height(70.dp))
            Icon(painter = painterResource(R.drawable.ic_set_up_pin), tint = Color.Unspecified, contentDescription = null)
            Spacer(modifier = Modifier.height(70.dp))
            Text(
                stringResource(
                    R.string.Set_up_Pin_question
                ), fontSize = 18.sp, fontWeight = FontWeight.W600, color = MixinAppTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Answer(stringResource(R.string.Set_up_Pin_answer_1), 0, selectedOption) { selectedOption = it }
            Spacer(modifier = Modifier.height(10.dp))
            Answer(stringResource(R.string.Set_up_Pin_answer_2), 1, selectedOption) { selectedOption = it }
            Spacer(modifier = Modifier.weight(1f))
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                onClick = {

                },
                enabled = selectedOption == 1,
                colors = ButtonDefaults.outlinedButtonColors(
                    backgroundColor = if (selectedOption == 1) MixinAppTheme.colors.accent else MixinAppTheme.colors.backgroundGray
                ),
                shape = RoundedCornerShape(32.dp),
                elevation = ButtonDefaults.elevation(
                    pressedElevation = 0.dp,
                    defaultElevation = 0.dp,
                    hoveredElevation = 0.dp,
                    focusedElevation = 0.dp,
                ),
            ) {
                Text(
                    text = stringResource(R.string.Done), color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(26.dp))
            Text(
                text = stringResource(R.string.What_is_Pin),
                color = MixinAppTheme.colors.textBlue
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun Answer(str: String, option: Int, selectedOption: Int, onOptionSelected: (Int) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MixinAppTheme.colors.backgroundWindow)
            .clickable {
                onOptionSelected(option)
            }
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        RadioButton(
            colors = RadioButtonDefaults.colors(
                selectedColor = MixinAppTheme.colors.accent,
                unselectedColor = MixinAppTheme.colors.textPrimary
            ),
            selected = (option == selectedOption), onClick = null, modifier = Modifier.size(16.dp)
        )
        Text(
            text = str, modifier = Modifier.padding(start = 10.dp), color = MixinAppTheme.colors.textPrimary, fontSize = 12.sp
        )
    }
}