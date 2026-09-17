package ru.greenland.crm.ui.update

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun UpdateBanner(viewModel: UpdateViewModel = hiltViewModel()) {
    val update by viewModel.availableUpdate.collectAsState()
    val isDownloading by viewModel.isDownloading.collectAsState()
    val info = update ?: return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.onBackground)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Доступно обновление ${info.versionName}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.weight(1f),
        )
        if (isDownloading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.background,
            )
        } else {
            TextButton(onClick = viewModel::install) {
                Text("Обновить", color = MaterialTheme.colorScheme.background)
            }
        }
    }
}
