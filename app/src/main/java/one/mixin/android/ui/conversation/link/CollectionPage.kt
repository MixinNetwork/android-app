package one.mixin.android.ui.conversation.link

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.request.ImageRequest
import one.mixin.android.R
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.inscription.compose.TextInscription
import one.mixin.android.ui.common.compose.SearchTextField
import one.mixin.android.vo.InscriptionItem
import one.mixin.android.widget.CoilRoundedHexagonTransformation

@Composable
fun CollectionPage(collectionHash: String, click: (InscriptionItem) -> Unit, onDismissRequest: () -> Unit) {
    val viewModel = hiltViewModel<CollectionViewModel>()
    val collectibles by viewModel.inscriptionItemsFlowByCollectionHash(collectionHash).collectAsState(initial = emptyList())
    val collection by viewModel.collectionFlowByHash(collectionHash).collectAsState(initial = null)
    val text = remember { mutableStateOf("") }

    val hint = stringResource(id = R.string.search_placeholder_inscription)

    val filteredCollectibles = collectibles.filter { collectible ->
        if (text.value.isBlank()) {
            true
        } else {
            collectible.sequence.toString().contains(text.value, true)
        }
    }

    MixinAppTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(topEnd = 8.dp, topStart = 8.dp))
                .background(MixinAppTheme.colors.background)

        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                CoilImage(
                    model = ImageRequest.Builder(LocalContext.current).data(collection?.iconURL).transformations(CoilRoundedHexagonTransformation()).build(),
                    modifier = Modifier
                        .width(36.dp)
                        .height(36.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    placeholder = R.drawable.ic_inscription_icon,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = stringResource(R.string.Send_Collectible),
                        style =
                        TextStyle(
                            fontWeight = FontWeight.W500,
                            color = MixinAppTheme.colors.textPrimary,
                            fontSize = 16.sp,
                        ),
                    )
                    Text(
                        text = collection?.description ?: "",
                        style =
                        TextStyle(
                            fontWeight = FontWeight.W400,
                            color = MixinAppTheme.colors.textAssist,
                            fontSize = 12.sp,
                        ),
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Image(
                    painter = painterResource(R.drawable.ic_circle_close),
                    modifier =
                    Modifier
                        .padding(horizontal = 8.dp)
                        .clip(CircleShape)
                        .clickable {
                            onDismissRequest()
                        },
                    contentDescription = null,
                )
            }
            Box(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MixinAppTheme.colors.backgroundWindow)
                    .padding(vertical = 8.dp)
            ) {
                SearchTextField(text, hint, color = Color.Transparent, h = null)
            }
            Spacer(modifier = Modifier.height(20.dp))
            if (collectibles.isEmpty()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_collectibles),
                        contentDescription = null,
                        tint = Color(0xFFD3D4D5),
                    )
                    Spacer(modifier = Modifier.height(22.dp))
                    Text(stringResource(R.string.NO_COLLECTIBLES), color = MixinAppTheme.colors.textRemarks)
                }
            } else if (filteredCollectibles.isEmpty()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_empty_file),
                        contentDescription = null,
                        tint = Color(0xFFD3D4D5),
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(stringResource(R.string.NO_RESULTS), color = MixinAppTheme.colors.textRemarks)
                }
            } else {
                LazyColumn {
                    items(filteredCollectibles) { collectible ->
                        CollectionItem(collection?.name ?: "", collectible, click)
                    }
                }
            }
        }
    }
}

@Composable
fun CollectionItem(name: String, inscription: InscriptionItem, click: (InscriptionItem) -> Unit) {
    Column {
        Row(
            modifier =
            Modifier
                .clickable {
                    click.invoke(inscription)
                }
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                Modifier
                    .size(42.dp)
                    .aspectRatio(1f),
            ) {
                if (inscription.isText) {
                    TextInscription(
                        inscription.contentURL, inscription.contentURL,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp)),
                        fontSize = 6.sp
                    )
                } else {
                    CoilImage(
                        model = inscription.contentURL,
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp)),
                        placeholder = R.drawable.ic_inscription_content,
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = name,
                    style =
                    TextStyle(
                        fontWeight = FontWeight.W400,
                        color = MixinAppTheme.colors.textPrimary,
                        fontSize = 14.sp,
                    ),
                )
                Text(
                    text = "#${inscription.sequence}",
                    style =
                    TextStyle(
                        fontWeight = FontWeight.W400,
                        color = MixinAppTheme.colors.textAssist,
                        fontSize = 12.sp,
                    ),
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}