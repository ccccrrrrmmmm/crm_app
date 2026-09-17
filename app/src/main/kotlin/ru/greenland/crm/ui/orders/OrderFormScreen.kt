package ru.greenland.crm.ui.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.greenland.crm.ui.components.DateTimePickerField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderFormScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: OrderFormViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.saved) {
        if (state.saved) onSaved()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Изменить заявку" else "Новая заявка") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ClientPicker(
                clients = state.clients,
                selectedId = state.clientId,
                onSelect = viewModel::onClientChange,
            )

            CategoryPicker(
                value = state.category,
                onValueChange = viewModel::onCategoryChange,
            )

            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("Что нужно сделать") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("Комментарий к заявке") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.materialsNote,
                onValueChange = viewModel::onMaterialsNoteChange,
                label = { Text("Что купить / уже куплено") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.price,
                onValueChange = viewModel::onPriceChange,
                label = { Text("Стоимость, ₽") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            MasterPicker(
                masters = state.masters,
                selectedId = state.masterId,
                onSelect = viewModel::onMasterChange,
            )

            DateTimePickerField(
                label = "Время начала",
                valueMillis = state.startedAt,
                onValueChange = viewModel::onStartedAtChange,
            )
            DateTimePickerField(
                label = "Время окончания",
                valueMillis = state.finishedAt,
                onValueChange = viewModel::onFinishedAtChange,
            )

            Button(
                onClick = viewModel::save,
                enabled = state.clientId != null && state.title.isNotBlank() && state.category.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Сохранить")
            }

            if (state.clients.isEmpty()) {
                Text(
                    text = "Сначала добавьте клиента на вкладке «Клиенты».",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CategoryPicker(value: String, onValueChange: (String) -> Unit) {
    Column {
        Text(
            text = "Тип работ",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(WorkCategories.presets) { preset ->
                FilterChip(
                    selected = value == preset,
                    onClick = { onValueChange(preset) },
                    label = { Text(preset) },
                )
            }
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("Или впишите свой вариант") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
    }
}

@Composable
private fun ClientPicker(
    clients: List<ru.greenland.crm.data.local.entity.ClientEntity>,
    selectedId: String?,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = clients.find { it.id == selectedId }?.fullName ?: "Выберите клиента"

    Column {
        Text(
            text = "Клиент",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box {
            TextButton(onClick = { expanded = true }) { Text(selectedName) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                clients.forEach { client ->
                    DropdownMenuItem(text = { Text(client.fullName) }, onClick = {
                        expanded = false
                        onSelect(client.id)
                    })
                }
            }
        }
    }
}

@Composable
private fun MasterPicker(
    masters: List<ru.greenland.crm.data.local.entity.MasterEntity>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = masters.find { it.id == selectedId }?.fullName ?: "Не назначен"

    Column {
        Text(
            text = "Мастер",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box {
            TextButton(onClick = { expanded = true }) { Text(selectedName) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text("Не назначен") }, onClick = {
                    expanded = false
                    onSelect(null)
                })
                masters.forEach { master ->
                    DropdownMenuItem(text = { Text(master.fullName) }, onClick = {
                        expanded = false
                        onSelect(master.id)
                    })
                }
            }
        }
    }
}
