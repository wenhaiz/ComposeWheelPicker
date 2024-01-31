package com.wenhaiz.lib.wheelpicker.datepicker

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.wenhaiz.lib.wheelpicker.PickerItemStringBuilder
import com.wenhaiz.lib.wheelpicker.WheelPicker
import com.wenhaiz.lib.wheelpicker.WheelPickerStyle
import kotlinx.coroutines.delay
import java.time.LocalDate

private const val TAG = "WheelDatePicker"

@Composable
fun WheelDatePicker(
    modifier: Modifier = Modifier,
    date: LocalDate,
    minDate: LocalDate = LocalDate.of(1970, 1, 1),
    maxDate: LocalDate = LocalDate.now(),
    pickerStyle: WheelPickerStyle = WheelPickerStyle.Default,
    selector: @Composable (BoxScope.() -> Unit)? = null,
    yearStringBuilder: PickerItemStringBuilder<Int>? = null,
    monthStringBuilder: PickerItemStringBuilder<Int>? = null,
    dayOfMonthStringBuilder: PickerItemStringBuilder<Int>? = null,
    yearRange: IntRange = IntRange(minDate.year, maxDate.year),
    onSelectDate: (date: LocalDate) -> Unit
) {
    require(minDate <= maxDate) { "minDate must be <= maxDate(minDate is $minDate,maxDate is $maxDate" }
    var initialDate by remember {
        mutableStateOf(date)
    }
    var autoRound: Boolean by remember {
        mutableStateOf(true)
    }
    LaunchedEffect(date) {
        Log.d(TAG, "WheelDatePicker:date $date")
        autoRound = false
        initialDate = date
        if (date !in minDate..maxDate) {
            delay(300)
            val rounded = date.coerceIn(minDate, maxDate)
            initialDate = rounded
            onSelectDate(rounded)
        }
        autoRound = true
    }

    Box(modifier = modifier) {
        val yearOptions: List<Int> by remember(yearRange) {
            val r = yearRange.toList()
            mutableStateOf(r)
        }
        val monthOptions: List<Int> by remember {
            mutableStateOf((1..12).toList())
        }
        val dayOfMonthOptions: List<Int> by remember(initialDate) {
            val r = monthOptions.map {
                val maxDayOfMonth = LocalDate.of(initialDate.year, it, 1).lengthOfMonth()
                (1..maxDayOfMonth).toList()
            }[initialDate.monthValue - 1]
            mutableStateOf(r)
        }

        val onChange: (Int?, Int?, Int?) -> Unit by rememberUpdatedState { year, month, dayOfMonth ->
            val tempDate = LocalDate.of(year ?: initialDate.year, month ?: initialDate.monthValue, 1)
            val newDate =
                LocalDate.of(
                    tempDate.year,
                    tempDate.monthValue,
                    (dayOfMonth ?: initialDate.dayOfMonth).coerceAtMost(tempDate.lengthOfMonth())
                ).coerceIn(minDate, maxDate)
            Log.d(
                TAG,
                "WheelDatePicker:init=$initialDate, changed year = $year,month=$month,dayOfMonth=$dayOfMonth, newDate=$newDate"
            )
            onSelectDate(newDate)
        }
        Row {
            WheelPicker(
                modifier = Modifier.weight(1f),
                options = yearOptions,
                initialValue = initialDate.year,
                formatter = yearStringBuilder,
                style = pickerStyle,
                minSelectableIndex = yearOptions.indexOf(minDate.year),
                maxSelectableIndex = yearOptions.indexOf(maxDate.year),
                autoRoundToSelectableRange = autoRound
            ) {
                //不能在这里capture initialDate,否则可能会造成以旧数据为base更新日期
                onChange(it, null, null)
            }
            WheelPicker(
                modifier = Modifier.weight(1f),
                options = monthOptions,
                initialValue = initialDate.monthValue,
                formatter = monthStringBuilder,
                style = pickerStyle,
                minSelectableIndex = if (initialDate.year == minDate.year) minDate.monthValue - 1 else 0,
                maxSelectableIndex = if (initialDate.year == maxDate.year) maxDate.monthValue - 1 else 11,
                autoRoundToSelectableRange = autoRound
            ) {
                onChange(null, it, null)
            }
            WheelPicker(
                modifier = Modifier.weight(1f),
                options = dayOfMonthOptions,
                initialValue = initialDate.dayOfMonth,
                formatter = dayOfMonthStringBuilder,
                style = pickerStyle,
                minSelectableIndex = if (initialDate.year == minDate.year && initialDate.monthValue == minDate.monthValue) minDate.dayOfMonth - 1 else 0,
                maxSelectableIndex = if (initialDate.year == maxDate.year && initialDate.monthValue == maxDate.monthValue) maxDate.dayOfMonth - 1 else dayOfMonthOptions.lastIndex,
                autoRoundToSelectableRange = autoRound
            ) {
                onChange(null, null, it)
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(pickerStyle.itemHeight)
        ) {
            selector?.invoke(this)
        }
    }
}