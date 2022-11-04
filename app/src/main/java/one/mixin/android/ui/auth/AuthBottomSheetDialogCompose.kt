@file:OptIn(ExperimentalPagerApi::class, ExperimentalPagerApi::class)

package one.mixin.android.ui.auth

import GlideImage
import androidx.collection.ArrayMap
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.ui.setting.ui.theme.MixinAppTheme
import one.mixin.android.vo.Scope
import one.mixin.android.vo.getScopeGroupName
import one.mixin.android.vo.groupScope

@Composable
fun AuthBottomSheetDialogCompose(
    name: String,
    iconUrl: String?,
    scopes: List<Scope>,
    onDismissRequest: () -> Unit,
    onConfirmed: (Set<Scope>) -> Unit
) {
    val scopeGroup = groupScope(scopes)
    MixinAppTheme {
        Column(
            modifier = Modifier
                .clip(shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .fillMaxWidth()
                .height(690.dp)
                .background(MixinAppTheme.colors.background)
                .padding(horizontal = 8.dp, vertical = 16.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_circle_close),
                modifier = Modifier
                    .align(alignment = Alignment.End)
                    .clip(CircleShape)
                    .clickable {
                        onDismissRequest()
                    },
                contentDescription = null
            )
            Text(
                stringResource(R.string.Authorizations),
                modifier = Modifier.align(alignment = CenterHorizontally),
                color = MixinAppTheme.colors.textPrimary,
                fontWeight = FontWeight.SemiBold, fontSize = 18.sp
            )
            Row(
                modifier = Modifier.align(alignment = CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (iconUrl != null) {
                    GlideImage(
                        data = iconUrl,
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape),
                        placeHolderPainter = painterResource(id = R.drawable.ic_avatar_place_holder)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                }
                Text(
                    name, color = MixinAppTheme.colors.textPrimary,
                )
            }
            ScopesContent(scopeGroup, scopes, onConfirmed)
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun ScopesContent(
    scopeGroup: ArrayMap<Int, MutableList<Scope>>,
    scopes: List<Scope>,
    onConfirmed: (Set<Scope>) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = 0)
    val scope = rememberCoroutineScope()
    val savedScopes = rememberSaveable { scopes.toMutableSet() }

    Column {
        HorizontalPager(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 16.dp, horizontal = 12.dp),
            state = pagerState, count = scopeGroup.size,
            verticalAlignment = Alignment.Top
        ) { page ->
            val groupId = scopeGroup.keyAt(page)
            val scopeItems = requireNotNull(scopeGroup[groupId])
            Column {
                Image(
                    painter = painterResource(groupId),
                    modifier = Modifier
                        .align(alignment = CenterHorizontally)
                        .size(80.dp),
                    contentDescription = null
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    getScopeGroupName(groupId),
                    modifier = Modifier.align(alignment = CenterHorizontally),
                    color = MixinAppTheme.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold, fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .clip(shape = RoundedCornerShape(8.dp))
                        .background(MixinAppTheme.colors.backgroundWindow)
                ) {
                    items(scopeItems) { scope ->
                        ScopeCheckLayout(scope, savedScopes.contains(scope)) { checked ->
                            if (checked) {
                                savedScopes.add(scope)
                            } else {
                                savedScopes.remove(scope)
                            }
                        }
                    }
                }
            }
        }
        if (scopeGroup.size > 1) {
            HorizontalPagerIndicator(
                pagerState = pagerState,
                modifier = Modifier
                    .align(CenterHorizontally),
                activeColor = MixinAppTheme.colors.accent
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        Button(
            modifier = Modifier
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
                    onConfirmed(savedScopes)
                }
            }
        ) {
            Text(
                stringResource(id = R.string.Next),
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ScopeCheckLayout(scope: Scope, state: Boolean, onCheckedChange: (checked: Boolean) -> Unit) {
    val checkedState = remember { mutableStateOf(state) }
    val isProfileScope = scope.source == Scope.SCOPES[0]
    Row(
        modifier = Modifier
            .run {
                if (!isProfileScope) {
                    clickable {
                        checkedState.value = !checkedState.value
                        onCheckedChange(checkedState.value)
                    }
                } else {
                    this
                }
            }
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()

    ) {
        Image(
            modifier = Modifier
                .padding(vertical = 3.dp)
                .padding(end = 8.dp),
            painter = painterResource(
                id = when {
                    isProfileScope ->
                        R.drawable.ic_selected_disable
                    checkedState.value ->
                        R.drawable.ic_selected
                    else ->
                        R.drawable.ic_not_selected
                }
            ),
            contentDescription = null
        )
        Column(
            modifier = Modifier.align(alignment = Alignment.Top)
        ) {
            Text(
                scope.name,
                fontSize = 16.sp,
                color = MixinAppTheme.colors.textPrimary
            )
            Text(
                scope.desc,
                fontSize = 14.sp,
                color = MixinAppTheme.colors.textSubtitle
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
        scopes = listOf(
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
            Scope.generateScopeFromString(context, "COLLECTIBLES:READ")
        ),
        {}, {}
    )
}
