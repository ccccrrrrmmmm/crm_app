package ru.greenland.crm.ui.broadcast

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BroadcastScreen(
    onBack: () -> Unit,
    viewModel: BroadcastViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.start()
        } else {
            Toast.makeText(context, "Без разрешения на SMS рассылка невозможна", Toast.LENGTH_LONG).show()
        }
    }

    fun startWithPermission() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) viewModel.start() else smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SMS-рассылка") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        val contentModifier = Modifier.fillMaxSize().padding(padding)
        if (state.sending || state.done) {
            ProgressContent(state = state, modifier = contentModifier, onDone = viewModel::reset)
        } else {
            ComposeContent(
                state = state,
                modifier = contentModifier,
                onMessageChange = viewModel::onMessageChange,
                onToggle = viewModel::toggle,
                onToggleAll = viewModel::toggleAll,
                onStart = ::startWithPermission,
            )
        }
    }
}

@Composable
private fun ComposeContent(
    state: BroadcastUiState,
    modifier: Modifier,
    onMessageChange: (String) -> Unit,
    onToggle: (String) -> Unit,
    onToggleAll: () -> Unit,
    onStart: () -> Unit,
) {
    Column(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = state.message,
            onValueChange = onMessageChange,
            label = { Text("Текст сообщения") },
            supportingText = { Text("{имя} — имя клиента, {фио} — полное имя") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Выбрано ${state.selectedCount} из ${state.reachable.size}",
                style = MaterialTheme.typography.titleSmall,
            )
            TextButton(onClick = onToggleAll, enabled = state.reachable.isNotEmpty()) {
                Text(if (state.allReachableSelected) "Снять всех" else "Выбрать всех")
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(state.clients, key = { it.id }) { client ->
                val hasPhone = client.phone.isNotBlank()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = hasPhone) { onToggle(client.id) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = hasPhone && client.id in state.selectedIds,
                        onCheckedChange = { onToggle(client.id) },
                        enabled = hasPhone,
                    )
                    Column(modifier = Modifier.padding(start = 4.dp)) {
                        Text(
                            text = client.fullName,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = if (hasPhone) client.phone else "нет телефона",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Button(
            onClick = onStart,
            enabled = state.message.isNotBlank() && state.selectedCount > 0,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Отправить всем по SMS (${state.selectedCount})")
        }
        Text(
            text = "Отправится автоматически всем выбранным. Каждое SMS платное по тарифу оператора.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProgressContent(
    state: BroadcastUiState,
    modifier: Modifier,
    onDone: () -> Unit,
) {
    Column(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val total = state.total
        val processed = state.sent + state.failed

        Spacer(Modifier.height(24.dp))
        if (state.done) {
            Text("SMS-рассылка завершена", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "Отправлено: ${state.sent} из $total" +
                    if (state.failed > 0) ", не удалось: ${state.failed}" else "",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text("Готово")
            }
        } else {
            Text("Отправка SMS…", style = MaterialTheme.typography.titleMedium)
            LinearProgressIndicator(
                progress = { if (total == 0) 0f else processed.toFloat() / total.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "$processed из $total",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
