package com.wenhaiz.lib.wheelpicker.datepicker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import com.wenhaiz.lib.wheelpicker.TwoLevelPicker
import java.time.LocalDate

@Composable
fun YearMonthPicker(
    modifier: Modifier = Modifier,
    minDate: LocalDate,
    maxDate: LocalDate,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val onChange: (Int?, Int?) -> Unit by rememberUpdatedState { year, month ->
        val tempDate = LocalDate.of(year ?: selectedDate.year, month ?: selectedDate.monthValue, 1)
        onDateSelected(tempDate)
    }
    TwoLevelPicker(
        modifier = modifier,
        leftOptions = (minDate.year..maxDate.year).toList(),
        rightOptions = (1..12).toList(),
        initialLeftValue = selectedDate.year,
        initialRightValue = selectedDate.monthValue,
        leftFormatter = { append("$it 年") },
        rightFormatter = { append("$it 月") },
        onLeftChange = {
            onChange(it, null)
        },
        onRightChange = {
            onChange(null, it)
        }
    )
}