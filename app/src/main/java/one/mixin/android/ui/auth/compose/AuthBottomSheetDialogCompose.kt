@file:OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalFoundationApi::class,
)

package one.mixin.android.ui.auth.compose

import android.annotation.SuppressLint
import androidx.collection.ArrayMap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.pager.HorizontalPagerIndicator
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.compose.GlideImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.vo.Scope
import one.mixin.android.vo.getScopeGroupIcon
import one.mixin.android.vo.getScopeGroupName
import one.mixin.android.vo.groupScope
import kotlin.math.abs

@Composable
fun AuthBottomSheetDialogCompose(
    name: String,
    iconUrl: String?,
    scopes: List<Scope>,
    onDismissRequest: (() -> Unit),
    step: AuthStep,
    errorContent: String,
    onResetClick: (() -> Unit)?,
    onConfirmed: ((List<String>) -> Unit)?,
    onBiometricClick: ((List<String>) -> Unit),
    onVerifyRequest: ((List<String>, String) -> Unit)?,
) {
    val scopeGroup = groupScope(scopes)
    val savedScopes = remember { scopes.toMutableSet() }

    MixinAppTheme {
        Column(
            modifier =
                Modifier
                    .clip(shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .fillMaxWidth()
                    .height(690.dp)
                    .background(MixinAppTheme.colors.background)
                    .padding(top = 16.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_circle_close),
                modifier =
                    Modifier
                        .align(alignment = Alignment.End)
                        .padding(horizontal = 8.dp)
                        .clip(CircleShape)
                        .clickable {
                            onDismissRequest()
                        },
                contentDescription = null,
            )
            Text(
                stringResource(R.string.Request_Authorization),
                modifier = Modifier.align(alignment = CenterHorizontally),
                color = MixinAppTheme.colors.textPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
            )
            Row(
                modifier =
                    Modifier
                        .align(alignment = CenterHorizontally)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (iconUrl != null) {
                    GlideImage(
                        data = iconUrl,
                        modifier =
                            Modifier
                                .size(16.dp)
                                .clip(CircleShape),
                        placeHolderPainter = painterResource(id = R.drawable.ic_avatar_place_holder),
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                }
                Text(
                    name,
                    color = MixinAppTheme.colors.textMinor,
                    fontSize = 14.sp,
                )
            }
            AnimatedContent(
                modifier = Modifier.weight(1f),
                targetState = step != AuthStep.DEFAULT,
                transitionSpec = {
                    (slideInHorizontally { it } togetherWith slideOutHorizontally { -it }).apply {
                        SizeTransform(clip = false)
                    }
                },
                label = "",
            ) { b ->
                if (b) {
                    val state: LazyListState = rememberLazyListState()
                    LazyColumn(
                        state = state,
                        modifier =
                            Modifier
                                .padding(vertical = 16.dp, horizontal = 32.dp)
                                .fillMaxWidth()
                                .wrapContentHeight(Alignment.Top)
                                .clip(shape = RoundedCornerShape(8.dp))
                                .background(MixinAppTheme.colors.backgroundWindow)
                                .verticalScrollbar(state, color = Color(0x99E5E7EB)),
                    ) {
                        items(scopes) { scope ->
                            ScopeCheckLayout(scope, savedScopes.contains(scope)) { checked ->
                                if (checked) {
                                    savedScopes.add(scope)
                                } else {
                                    savedScopes.remove(scope)
                                }
                            }
                        }
                    }
                } else {
                    ScopesContent(scopeGroup, savedScopes, onConfirmed = onConfirmed)
                }
            }
            AnimatedVisibility(
                visible = step != AuthStep.DEFAULT,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
            ) {
                PinKeyBoard(step, errorContent, onResetClick = onResetClick, onBiometricClick = {
                    onBiometricClick(savedScopes.map { it.source })
                }) { pin ->
                    onVerifyRequest?.invoke(savedScopes.map { it.source }, pin)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScopesContent(
    scopeGroup: ArrayMap<Int, MutableList<Scope>>,
    scopes: MutableSet<Scope>,
    onConfirmed: ((List<String>) -> Unit)?,
) {
    val pagerState =
        rememberPagerState(
            initialPage = 0,
            initialPageOffsetFraction = 0f,
            pageCount = { scopeGroup.size },
        )
    val scope = rememberCoroutineScope()
    Column {
        HorizontalPager(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(vertical = 16.dp, horizontal = 4.dp),
            state = pagerState,
            verticalAlignment = Alignment.Top,
        ) { page ->
            val groupId = scopeGroup.keyAt(page)
            val scopeItems = requireNotNull(scopeGroup[groupId])
            Column {
                Image(
                    painter = painterResource(getScopeGroupIcon(groupId)),
                    modifier =
                        Modifier
                            .align(alignment = CenterHorizontally)
                            .size(80.dp),
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(id = getScopeGroupName(groupId)),
                    modifier = Modifier.align(alignment = CenterHorizontally),
                    color = MixinAppTheme.colors.textPrimary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp,
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    modifier =
                        Modifier
                            .padding(horizontal = 32.dp, vertical = 16.dp)
                            .fillMaxWidth()
                            .clip(shape = RoundedCornerShape(8.dp))
                            .background(MixinAppTheme.colors.backgroundWindow),
                ) {
                    items(scopeItems) { scope ->
                        ScopeCheckLayout(scope, scopes.contains(scope)) { checked ->
                            if (checked) {
                                scopes.add(scope)
                            } else {
                                scopes.remove(scope)
                            }
                        }
                    }
                }
            }
        }
        if (scopeGroup.size > 1) {
            HorizontalPagerIndicator(
                pagerState = pagerState,
                pageCount = scopeGroup.size,
                modifier =
                    Modifier
                        .align(CenterHorizontally),
                activeColor = MixinAppTheme.colors.accent,
                inactiveColor = MixinAppTheme.colors.backgroundGray,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        Button(
            modifier =
                Modifier
                    .align(CenterHorizontally),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = MixinAppTheme.colors.accent),
            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 28.dp),
            onClick = {
                if (pagerState.currentPage < scopeGroup.keys.size - 1) {
                    scope.launch {
                        pagerState.animateScrollToPage(page = pagerState.currentPage + 1)
                    }
                } else {
                    onConfirmed?.invoke(scopes.map { it.source })
                }
            },
        ) {
            Text(
                stringResource(id = R.string.Next),
                fontSize = 16.sp,
                color = Color.White,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@SuppressLint("ComposableModifierFactory")
@Composable
fun Modifier.verticalScrollbar(
    state: LazyListState,
    width: Dp = 4.dp,
    color: Color = MixinAppTheme.colors.accent.copy(ContentAlpha.disabled),
): Modifier {
    return drawWithContent {
        drawContent()

        val firstVisibleElementInfo = state.layoutInfo.visibleItemsInfo.firstOrNull()

        val visibleSize =
            state.layoutInfo.visibleItemsInfo.sumOf {
                it.size
            }.toFloat()

        if (visibleSize == this.size.height) {
            // Do nothing.
        } else if (firstVisibleElementInfo != null) {
            val elementHeight = this.size.height / state.layoutInfo.totalItemsCount
            val scrollbarOffsetY =
                (firstVisibleElementInfo.index + (abs(firstVisibleElementInfo.offset).toFloat() / firstVisibleElementInfo.size)) * elementHeight
            val scrollbarHeight = state.layoutInfo.visibleItemsInfo.size * elementHeight

            drawRoundRect(
                color = color,
                cornerRadius = CornerRadius(8f, 8f),
                topLeft = Offset(this.size.width - width.toPx(), scrollbarOffsetY),
                size = Size(width.toPx(), scrollbarHeight),
            )
        }
    }
}

@Composable
@Preview
fun AuthBottomSheetDialogComposePreview() {
    val context = LocalContext.current
    AuthBottomSheetDialogCompose(
        name = "Team Mixin",
        iconUrl = "https://mixin-images.zeromesh.net/E2y0BnTopFK9qey0YI-8xV3M82kudNnTaGw0U5SU065864SsewNUo6fe9kDF1HIzVYhXqzws4lBZnLj1lPsjk-0=s256",
        scopes =
            listOf(
                Scope.generateScopeFromString(context, "PROFILE:READ"),
                Scope.generateScopeFromString(context, "PHONE:READ"),
                Scope.generateScopeFromString(context, "MESSAGES:REPRESENT"),
                Scope.generateScopeFromString(context, "CONTACTS:READ"),
                Scope.generateScopeFromString(context, "ASSETS:READ"),
                Scope.generateScopeFromString(context, "SNAPSHOTS:READ"),
                Scope.generateScopeFromString(context, "APPS:READ"),
                Scope.generateScopeFromString(context, "APPS:WRITE"),
                Scope.generateScopeFromString(context, "CIRCLES:READ"),
                Scope.generateScopeFromString(context, "CIRCLES:WRITE"),
                Scope.generateScopeFromString(context, "COLLECTIBLES:READ"),
            ),
        {},
        AuthStep.INPUT,
        "",
        {},
        {},
        {},
        null,
    )
}
