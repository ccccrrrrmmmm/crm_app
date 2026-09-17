package ru.greenland.crm.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.greenland.crm.data.local.entity.OrderStatus

@Composable
fun StatusBadge(status: OrderStatus, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(6.dp)
    when (status) {
        OrderStatus.DONE -> {
            Text(
                text = status.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.background,
                modifier = modifier
                    .background(MaterialTheme.colorScheme.onBackground, shape)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        OrderStatus.CANCELLED -> {
            Text(
                text = status.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = modifier
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), shape)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        else -> {
            Text(
                text = status.label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (status == OrderStatus.IN_PROGRESS) FontWeight.Bold else FontWeight.Medium,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = modifier
                    .border(BorderStroke(1.5.dp, MaterialTheme.colorScheme.onBackground), shape)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}
