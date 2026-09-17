package ru.greenland.crm.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

private val displayFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

fun formatDateTime(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(displayFormatter)

/** Поле выбора даты и времени вручную (два системных диалога подряд: дата → время). */
@Composable
fun DateTimePickerField(
    label: String,
    valueMillis: Long?,
    onValueChange: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = {
                calendar.timeInMillis = valueMillis ?: System.currentTimeMillis()
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH)
                val day = calendar.get(Calendar.DAY_OF_MONTH)
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                val minute = calendar.get(Calendar.MINUTE)

                DatePickerDialog(
                    context,
                    { _, pickedYear, pickedMonth, pickedDay ->
                        TimePickerDialog(
                            context,
                            { _, pickedHour, pickedMinute ->
                                val result = Calendar.getInstance().apply {
                                    set(pickedYear, pickedMonth, pickedDay, pickedHour, pickedMinute, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                onValueChange(result.timeInMillis)
                            },
                            hour,
                            minute,
                            true,
                        ).show()
                    },
                    year,
                    month,
                    day,
                ).show()
            }) {
                Text(valueMillis?.let(::formatDateTime) ?: "Не указано")
            }
            if (valueMillis != null) {
                IconButton(onClick = { onValueChange(null) }) {
                    Icon(Icons.Filled.Close, contentDescription = "Очистить")
                }
            }
        }
    }
}
