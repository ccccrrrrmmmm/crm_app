package ru.greenland.crm.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.greenland.crm.data.sync.MIN_SYNC_INTERVAL_MINUTES
import ru.greenland.crm.ui.components.formatDateTime

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (uiState.isEditing) {
            EditingForm(uiState = uiState, viewModel = viewModel)
        } else {
            ConnectedCard(uiState = uiState, viewModel = viewModel)
        }
    }
}

@Composable
private fun EditingForm(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    val form = uiState.form

    OutlinedTextField(
        value = form.owner,
        onValueChange = viewModel::onOwnerChange,
        label = { Text("Владелец репозитория (логин на GitHub)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = form.repo,
        onValueChange = viewModel::onRepoChange,
        label = { Text("Название репозитория") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = form.branch,
        onValueChange = viewModel::onBranchChange,
        label = { Text("Ветка (можно оставить пустым)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = form.token,
        onValueChange = viewModel::onTokenChange,
        label = { Text("Personal Access Token") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = form.intervalMinutes,
        onValueChange = viewModel::onIntervalChange,
        label = { Text("Интервал автосинхронизации, мин (не меньше $MIN_SYNC_INTERVAL_MINUTES)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = viewModel::connect,
            enabled = uiState.testState != ConnectionTestState.TESTING,
            modifier = Modifier.weight(1f),
        ) {
            Text("Подключиться")
        }
        if (uiState.isConfigured) {
            OutlinedButton(onClick = viewModel::cancelEditing) {
                Text("Отмена")
            }
        }
    }

    when (uiState.testState) {
        ConnectionTestState.TESTING -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Text(
                text = "  Проверяю доступ...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ConnectionTestState.ERROR -> Text(
            text = uiState.testMessage ?: "Не удалось подключиться",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        else -> Unit
    }
}

@Composable
private fun ConnectedCard(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    val form = uiState.form

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(
                    text = "  Подключено",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = "${form.owner}/${form.repo}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                text = "Ветка: ${form.branch}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(modifier = Modifier.padding(top = 10.dp)) {
                TextButton(onClick = viewModel::startEditing) { Text("Изменить") }
                TextButton(onClick = viewModel::disconnect) { Text("Отключить") }
            }
        }
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

    val lastSuccessAt = uiState.status.lastSuccessAt
    Text(text = "Синхронизация", style = MaterialTheme.typography.titleMedium)
    Text(
        text = when {
            uiState.status.isSyncing -> "Синхронизация..."
            uiState.status.lastError != null -> "Ошибка: ${uiState.status.lastError}"
            lastSuccessAt != null -> "Последняя синхронизация: ${formatDateTime(lastSuccessAt)}"
            else -> "Ещё не синхронизировалось"
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedButton(
        onClick = viewModel::syncNow,
        enabled = !uiState.status.isSyncing,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Синхронизировать сейчас")
    }
}
