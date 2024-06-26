package one.mixin.android.ui.home.inscription.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.reflect.TypeToken
import one.mixin.android.R
import one.mixin.android.util.GsonHelper

@Composable
fun WrappingRow(
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = 12.dp,
    verticalSpacing: Dp = 12.dp,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val rowConstraints = constraints.copy(minWidth = 0)
        val placeables = measurables.map { measurable ->
            measurable.measure(rowConstraints)
        }

        var currentRowWidth = 0
        var currentRowHeight = 0
        var totalHeight = 0
        val rowHeights = mutableListOf<Int>()
        val rowItems = mutableListOf<MutableList<Placeable>>()
        var currentRowItems = mutableListOf<Placeable>()

        placeables.forEach { placeable ->
            if (currentRowWidth + placeable.width + (if (currentRowItems.isEmpty()) 0 else horizontalSpacing.roundToPx()) > constraints.maxWidth) {
                rowHeights.add(currentRowHeight)
                rowItems.add(currentRowItems)

                totalHeight += currentRowHeight + verticalSpacing.roundToPx()
                currentRowHeight = 0
                currentRowWidth = 0
                currentRowItems = mutableListOf()
            }
            if (currentRowItems.isNotEmpty()) {
                currentRowWidth += horizontalSpacing.roundToPx()
            }
            currentRowItems.add(placeable)
            currentRowWidth += placeable.width
            currentRowHeight = maxOf(currentRowHeight, placeable.height)
        }

        rowHeights.add(currentRowHeight)
        rowItems.add(currentRowItems)
        totalHeight += currentRowHeight

        layout(constraints.maxWidth, totalHeight) {
            var yPosition = 0
            rowItems.forEachIndexed { index, items ->
                var xPosition = 0
                items.forEachIndexed { itemIndex, placeable ->
                    placeable.placeRelative(x = xPosition, y = yPosition)
                    xPosition += placeable.width
                    if (itemIndex < items.size - 1) {
                        xPosition += horizontalSpacing.roundToPx()
                    }
                }
                yPosition += rowHeights[index] + verticalSpacing.roundToPx()
            }
        }
    }
}


@Composable
fun TraitChip(trait: Trait) {
    Column(
        modifier = Modifier
            .border(
                width = 1.dp,
                brush = SolidColor(Color(0xFF999999)),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .wrapContentSize()
    ) {
        Text(trait.name, color = Color(0xFF999999), fontSize = 12.sp)
        Spacer(modifier = Modifier.height(1.dp))
        Text(trait.value, color = Color.White, fontSize = 14.sp)
    }
}

@Composable
fun TraitsRow(json: String?) {
    json ?: return
    val list = parseJsonToTraitList(json)
    if (list.isNullOrEmpty()) return
    Column {
        Box(modifier = Modifier.height(20.dp))
        Text(text = stringResource(id = R.string.Traits).uppercase(), fontSize = 16.sp, color = Color(0xFF999999))
        Box(modifier = Modifier.height(8.dp))
        WrappingRow {
            list.forEach { trait ->
                TraitChip(trait)
            }
        }
    }
}

data class Trait(
    val name: String,
    val value: String
)

fun parseJsonToTraitList(jsonString: String): List<Trait>? {
    val gson = GsonHelper.customGson
    try {
        val listType = object : TypeToken<List<Trait>>() {}.type
        val traitList: List<Trait> = gson.fromJson(jsonString, listType)
        return traitList
    } catch (e: Exception) {
        return null
    }
}


