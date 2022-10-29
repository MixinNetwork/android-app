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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.ui.setting.ui.theme.MixinAppTheme
import one.mixin.android.vo.Scope
import one.mixin.android.vo.getScopeGroupName
import one.mixin.android.vo.groupScope
import timber.log.Timber

@Composable
fun AuthBottomSheetDialogCompose(
    name: String,
    iconUrl: String?,
    scopes: List<Scope>,
    onDismissRequest: () -> Unit,
    onConfirmed: () -> Unit
) {
    val scopeGroup = groupScope(scopes)
    MixinAppTheme {
        Column(
            modifier = Modifier
                .clip(shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .fillMaxWidth()
                .height(560.dp)
                .background(MixinAppTheme.colors.background)
                .padding(16.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_circle_close),
                modifier = Modifier
                    .align(alignment = Alignment.End)
                    .clip(CircleShape)
                    .clickable {
                        // Todo
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
                }
                Text(
                    name, color = MixinAppTheme.colors.textPrimary,
                )
            }
            ScopesContent(scopeGroup)
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun ScopesContent(scopeGroup: ArrayMap<Int, MutableList<Scope>>) {
    val pagerState = rememberPagerState(initialPage = 0)
    val scope = rememberCoroutineScope()
    Column {
        HorizontalPager(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            state = pagerState, count = scopeGroup.size,
            verticalAlignment = Alignment.Top
        ) { page ->
            val groupId = scopeGroup.keyAt(page)
            val scopes = requireNotNull(scopeGroup[groupId])
            Column {
                Spacer(modifier = Modifier.height(16.dp))
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
                    items(scopes) { scope ->
                        ScopeCheckLayout(scope)
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
            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 24.dp),
            onClick = {
                if (pagerState.currentPage < scopeGroup.keys.size) {
                    scope.launch {
                        pagerState.animateScrollToPage(page = pagerState.currentPage + 1)
                    }
                } else {
                    Timber.e("Done")
                }
            }
        ) {
            Text(
                stringResource(id = R.string.Next),
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun ScopeCheckLayout(scope: Scope) {
    val checkedState = remember { mutableStateOf(true) }
    Row(
        modifier = Modifier
            .clickable {
                checkedState.value = !checkedState.value
            }
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()

    ) {
        Image(
            modifier = Modifier
                .padding(vertical = 3.dp)
                .padding(end = 8.dp),
            painter = painterResource(
                id = if (checkedState.value) {
                    R.drawable.ic_selected
                } else {
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
                fontSize = 14.sp,
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
    AuthBottomSheetDialogCompose(
        name = "Team Mixin",
        iconUrl = "https://mixin-images.zeromesh.net/E2y0BnTopFK9qey0YI-8xV3M82kudNnTaGw0U5SU065864SsewNUo6fe9kDF1HIzVYhXqzws4lBZnLj1lPsjk-0=s256",
        scopes = listOf(
            Scope.generateScopeFromString(MixinApplication.appContext, "PROFILE:READ"),
            Scope.generateScopeFromString(MixinApplication.appContext, "PHONE:READ"),
            Scope.generateScopeFromString(MixinApplication.appContext, "MESSAGES:REPRESENT"),
            Scope.generateScopeFromString(MixinApplication.appContext, "CONTACTS:READ"),
            Scope.generateScopeFromString(MixinApplication.appContext, "ASSETS:READ"),
            Scope.generateScopeFromString(MixinApplication.appContext, "SNAPSHOTS:READ"),
            Scope.generateScopeFromString(MixinApplication.appContext, "APPS:READ"),
            Scope.generateScopeFromString(MixinApplication.appContext, "APPS:WRITE"),
            Scope.generateScopeFromString(MixinApplication.appContext, "CIRCLES:READ"),
            Scope.generateScopeFromString(MixinApplication.appContext, "CIRCLES:WRITE"),
            Scope.generateScopeFromString(MixinApplication.appContext, "COLLECTIBLES:READ")
        ),
        {}, {}
    )
}
