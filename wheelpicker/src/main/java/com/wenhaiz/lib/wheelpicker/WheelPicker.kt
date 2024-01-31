package com.wenhaiz.lib.wheelpicker

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlin.math.absoluteValue

private const val DEBUG_TAG = "WheelPicker"

@Stable
data class WheelPickerStyle(
    val itemHeight: Dp = 36.dp,
    private val rowCount: Int = 5,
    val unSelectableColor: Color = Color.LightGray,
    val textColor: Color = Color.Black,
    val textStyle: TextStyle = TextStyle.Default
) {
    //奇数
    val adjustedRowCount: Int = if (rowCount.and(1) == 0) rowCount + 1 else rowCount

    companion object {
        val Default = WheelPickerStyle()
    }
}

typealias PickerItemStringBuilder<T> = AnnotatedString.Builder.(T) -> Unit

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> WheelPicker(
    modifier: Modifier = Modifier,
    options: List<T>,
    initialValue: T?,
    style: WheelPickerStyle = WheelPickerStyle.Default,
    formatter: PickerItemStringBuilder<T>? = null,
    key: (T) -> Any = { it.toString() },
    minSelectableIndex: Int = 0,
    maxSelectableIndex: Int = options.lastIndex,
    selector: @Composable (BoxScope.() -> Unit)? = null,
    autoRoundToSelectableRange: Boolean = true,
    onValueChange: (T) -> Unit
) {
    val initialIndex = options.indexOf(initialValue)
    val validInitialIndex = if (autoRoundToSelectableRange) {
        initialIndex.coerceIn(minSelectableIndex, maxSelectableIndex)
    } else {
        initialIndex.takeIf { it >= 0 } ?: minSelectableIndex
    }
    val lazyListState = rememberLazyListState(validInitialIndex)
    LaunchedEffect(validInitialIndex) {
        lazyListState.stopScroll(MutatePriority.PreventUserInput)
        lazyListState.animateScrollToItem(validInitialIndex)
    }
    LaunchedEffect(lazyListState, initialIndex, options, minSelectableIndex, maxSelectableIndex) {
        snapshotFlow {
            lazyListState.isScrollInProgress to lazyListState.layoutInfo
        }.map {
            val (isScroll, layoutInfo) = it
            if (!isScroll) {
                val item = layoutInfo.visibleItemsInfo.firstOrNull { info ->
                    Log.d(
                        DEBUG_TAG,
                        "WheelPicker: ${info.offset} ${info.size} viewport size=${layoutInfo.viewportSize}}"
                    )
                    //发现可能有-1的case，itemHeight为奇数像素时，round导致的问题
                    info.offset.absoluteValue <= 1
                }
                item?.index
            } else {
                null
            }
        }.filterNotNull()
            .collectLatest { newIndex ->
                val newIndex0 = if (autoRoundToSelectableRange) {
                    newIndex.coerceIn(minSelectableIndex, maxSelectableIndex)
                } else {
                    newIndex
                }
                Log.d(
                    DEBUG_TAG,
                    "WheelPicker: curSelectionIndex=${newIndex0},newIndex=$newIndex,initialIndex=$initialIndex,initialValue=$initialValue"
                )
                if (newIndex0 != newIndex) {
                    lazyListState.animateScrollToItem(newIndex0)
                } else if (initialValue == null || newIndex0 != initialIndex) {
                    onValueChange(options[newIndex0])
                }
            }
    }
    val snapOffset = with(LocalDensity.current) {
        (style.itemHeight * (style.adjustedRowCount - 1) / 2).roundToPx()
    }
    Box(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(style.itemHeight * style.adjustedRowCount),
            state = lazyListState,
            flingBehavior = rememberSnapFlingBehavior(remember {
                SnapLayoutInfoProvider(lazyListState)
            }),
            contentPadding = PaddingValues(vertical = style.itemHeight * (style.adjustedRowCount - 1) / 2),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            itemsIndexed(options, key = { _, item -> key(item) }) { index, option ->
                Box(
                    modifier = Modifier
                        .height(style.itemHeight)
                        .fillMaxWidth()
                ) {
                    val text = buildAnnotatedString {
                        if (formatter != null) {
                            formatter.invoke(this, option)
                        } else {
                            append(option.toString())
                        }
                    }
                    Text(
                        text = text,
                        style = style.textStyle.copy(
                            color = if (index in minSelectableIndex..maxSelectableIndex) style.textColor else style.unSelectableColor
                        ),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .graphicsLayer {
                                val itemLayoutInfo = lazyListState.layoutInfo.visibleItemsInfo
                                    .firstOrNull {
                                        it.index == index
                                    }
                                itemLayoutInfo?.let {
                                    this.transformOrigin =
                                        TransformOrigin(0.5f, if (it.offset < 0) 1f else 0f)
                                    val ratio = it.offset / snapOffset.toFloat()
                                    this.alpha = 1 - ratio.absoluteValue * 0.8f
//                                    Log.d(
//                                        DEBUG_TAG,
//                                        "WheelPicker: layout= $index, offset=${it.offset}, ratio=${ratio}"
//                                    )
                                    this.rotationX = 36 * -ratio
                                }
                            },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(style.itemHeight)
        ) {
            selector?.invoke(this)
        }
    }
}