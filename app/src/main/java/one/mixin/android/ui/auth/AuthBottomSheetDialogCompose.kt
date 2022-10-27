@file:OptIn(ExperimentalPagerApi::class, ExperimentalPagerApi::class)

package one.mixin.android.ui.auth

import androidx.collection.ArrayMap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.ui.setting.ui.compose.AppAvatarImage
import one.mixin.android.ui.setting.ui.theme.MixinAppTheme
import one.mixin.android.vo.App
import one.mixin.android.vo.Scope
import one.mixin.android.vo.getScopeGroupName
import one.mixin.android.vo.groupScope
import timber.log.Timber

@Composable
fun AuthBottomSheetDialogCompose(app: App, scopes: List<Scope>) {
    val scopeGroup = groupScope(scopes)

    MixinAppTheme {

        Column(
            modifier = Modifier
                .clip(shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .fillMaxWidth()
                .fillMaxHeight()
                .background(MixinAppTheme.colors.background)
                .padding(16.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_circle_close),
                modifier = Modifier.align(alignment = Alignment.End),
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
                AppAvatarImage(
                    app = app,
                    size = 16.dp
                )
                Text(
                    app.name, color = MixinAppTheme.colors.textPrimary,
                )
            }
            ScopeWidget(scopeGroup)
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun ScopeWidget(scopeGroup: ArrayMap<Int, MutableList<Scope>>) {
    val pagerState = rememberPagerState(initialPage = 0)
    val scope = rememberCoroutineScope()
    Column {
        HorizontalPager(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp), state = pagerState, count = scopeGroup.size,
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
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    getScopeGroupName(groupId),
                    modifier = Modifier.align(alignment = CenterHorizontally),
                    color = MixinAppTheme.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold, fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .clip(shape = RoundedCornerShape(8.dp))
                        .background(MixinAppTheme.colors.backgroundWindow)
                        .padding(16.dp)
                ) {
                    items(scopes) { scope ->
                        Column {
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
            }
        }
        HorizontalPagerIndicator(
            pagerState = pagerState,
            modifier = Modifier
                .align(CenterHorizontally),
            activeColor = MixinAppTheme.colors.accent
        )
        Spacer(modifier = Modifier.height(16.dp))
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
            }) {
            Text(
                stringResource(id = R.string.Next),
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(30.dp))
    }
}
