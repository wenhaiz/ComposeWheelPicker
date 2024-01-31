package com.wenhaiz.lib.wheelpicker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier


//wip: 未完成
@Composable
fun <T, S> SubLevelWheelPicker(
    modifier: Modifier = Modifier,
    options: List<T>,
    initialValue: T,
    childrenBuilder: (T) -> Pair<S?, List<S>>,
    onLeftValueChange: (T) -> Unit,
    onRightValueChange: (S) -> Unit
) {
    val (initialChildren, children) = childrenBuilder(initialValue)

    TwoLevelPicker(
        modifier = modifier,
        leftOptions = options,
        rightOptions = children,
        initialLeftValue = initialValue,
        initialRightValue = initialChildren ?: children.first(),
        onLeftChange = {
            onLeftValueChange(it)
        },
        onRightChange = {
            onRightValueChange(it)
        }
    )
}


@Composable
fun <T, S> TwoLevelPicker(
    modifier: Modifier = Modifier,
    pickerStyle: WheelPickerStyle = WheelPickerStyle.Default,
    leftOptions: List<T>,
    rightOptions: List<S>,
    initialLeftValue: T,
    initialRightValue: S,
    leftFormatter: PickerItemStringBuilder<T>? = null,
    rightFormatter: PickerItemStringBuilder<S>? = null,
    onLeftChange: (T) -> Unit,
    onRightChange: (S) -> Unit
) {
    Box(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth()) {
            WheelPicker(
                modifier = Modifier.weight(1f),
                options = leftOptions,
                initialValue = initialLeftValue,
                style = pickerStyle,
                formatter = leftFormatter
            ) {
                onLeftChange(it)
            }
            WheelPicker(
                modifier = Modifier.weight(1f),
                options = rightOptions,
                initialValue = initialRightValue,
                style = pickerStyle,
                formatter = rightFormatter
            ) {
                onRightChange(it)
            }
        }
        DefaultPickerSelector(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(pickerStyle.itemHeight)
        )
    }
}