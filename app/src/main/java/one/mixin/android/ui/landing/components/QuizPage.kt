package one.mixin.android.ui.landing.components

import PageScaffold
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontWeight.Companion.W500
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.openUrl

@Composable
fun QuizPage(next: () -> Unit, pop: (() -> Unit)? = null) {
    val context = LocalContext.current
    var selectedOption by remember { mutableStateOf(-1) }
    var showDialog by remember { mutableStateOf(false) }
    var isCorrectAnswer by remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true,
    )
    val coroutineScope = rememberCoroutineScope()
    
    LaunchedEffect(showDialog) {
        if (showDialog) {
            bottomSheetState.show()
        }
    }
    
    ModalBottomSheetLayout(
        sheetState = bottomSheetState,
        scrimColor = Color.Black.copy(alpha = 0.6f),
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        sheetBackgroundColor = MixinAppTheme.colors.background,
        sheetContent = {
            QuizResultBottomSheetContent(
                isCorrect = isCorrectAnswer,
                onCorrectAction = {
                    coroutineScope.launch {
                        bottomSheetState.hide()
                        showDialog = false
                        next()
                    }
                },
                onWrongAction = {
                    coroutineScope.launch {
                        bottomSheetState.hide()
                        showDialog = false
                        selectedOption = -1
                    }
                }
            )
        }
    ) {
        PageScaffold(
            title = "",
            verticalScrollable = false,
            actions = {
                IconButton(onClick = {
                    context.openUrl(Constants.HelpLink.CUSTOMER_SERVICE)
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_support),
                        contentDescription = null,
                        tint = MixinAppTheme.colors.icon,
                    )
                }
            },
            pop = pop,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
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
                            if (selectedOption != -1) {
                                isCorrectAnswer = selectedOption == 1
                                showDialog = true
                            }
                        },
                        enabled = selectedOption != -1,
                        colors = ButtonDefaults.outlinedButtonColors(
                            backgroundColor = if (selectedOption != -1) MixinAppTheme.colors.accent else MixinAppTheme.colors.backgroundGray
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
                            text = stringResource(R.string.Check_answer), color = Color.White, fontSize = 14.sp, fontWeight = W500,
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.What_is_Pin),
                        fontSize = 14.sp,
                        fontWeight = W500,
                        color = MixinAppTheme.colors.textBlue,
                        modifier = Modifier.clickable {
                            context.openUrl(context.getString(R.string.What_is_Pin_url))
                        }
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
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
            text = str, modifier = Modifier.padding(start = 10.dp), color = MixinAppTheme.colors.textPrimary, fontSize = 14.sp
        )
    }
}

@Composable
fun QuizResultBottomSheetContent(
    isCorrect: Boolean,
    onCorrectAction: () -> Unit,
    onWrongAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .background(MixinAppTheme.colors.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(65.dp))
        
        Box(
            modifier = Modifier.size(70.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(
                    if (isCorrect) R.drawable.ic_order_success else R.drawable.ic_order_failed
                ),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(70.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = stringResource(if (isCorrect) R.string.Quiz_correct else R.string.Quiz_wrong_answer),
            fontSize = 20.sp,
            fontWeight = FontWeight.W600,
            color = MixinAppTheme.colors.textPrimary
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = stringResource(R.string.Quiz_pin_explanation),
            fontSize = 14.sp,
            color = MixinAppTheme.colors.textAssist,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        
        Spacer(modifier = Modifier.height(75.dp))
        
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            onClick = if (isCorrect) onCorrectAction else onWrongAction,
            colors = ButtonDefaults.outlinedButtonColors(
                backgroundColor = MixinAppTheme.colors.accent
            ),
            shape = RoundedCornerShape(24.dp),
            elevation = ButtonDefaults.elevation(
                pressedElevation = 0.dp,
                defaultElevation = 0.dp,
                hoveredElevation = 0.dp,
                focusedElevation = 0.dp,
            ),
        ) {
            Text(
                text = stringResource(if (isCorrect) R.string.Got_it else R.string.Try_Again),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.W500
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}