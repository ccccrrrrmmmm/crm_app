package ru.greenland.crm.ui.orders

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import java.io.File
import ru.greenland.crm.data.local.entity.MasterEntity
import ru.greenland.crm.data.local.entity.MessageEntity
import ru.greenland.crm.data.local.entity.OrderPhotoEntity
import ru.greenland.crm.data.local.entity.OrderStatus
import ru.greenland.crm.ui.components.StatusBadge
import ru.greenland.crm.ui.components.formatDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    viewModel: OrderDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val draft by viewModel.draftText.collectAsState()
    var confirmDelete by remember { mutableStateOf(false) }
    var previewPhoto by remember { mutableStateOf<OrderPhotoEntity?>(null) }
    var pendingPhotoFile by remember { mutableStateOf<File?>(null) }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) pendingPhotoFile?.let(viewModel::confirmPhotoCaptured)
        pendingPhotoFile = null
    }
    val pickFromGallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(viewModel::attachPhotoFromGallery)
    }

    val order = uiState.order

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(order?.title ?: "Заявка", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    order?.let {
                        TextButton(onClick = { onEdit(it.id) }) { Text("Изменить") }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Удалить заявку")
                        }
                    }
                },
            )
        },
        bottomBar = {
            MessageInputBar(
                value = draft,
                onValueChange = viewModel::onDraftChange,
                onSend = viewModel::sendMessage,
            )
        },
    ) { padding ->
        if (order == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                OrderSummary(
                    clientName = uiState.client?.fullName ?: "—",
                    clientPhone = uiState.client?.phone,
                    clientAddress = uiState.client?.address,
                    category = order.category,
                    description = order.description,
                    materialsNote = order.materialsNote,
                    price = order.price,
                    status = order.status,
                    masterName = uiState.master?.fullName,
                    masters = uiState.masters,
                    startedAt = order.startedAt,
                    finishedAt = order.finishedAt,
                    onStatusChange = viewModel::setStatus,
                    onMasterChange = viewModel::assignMaster,
                )
            }
            item { HorizontalDivider() }
            item {
                PhotosSection(
                    photos = uiState.photos,
                    onAddPhoto = {
                        val (file, uri) = viewModel.preparePhotoCapture()
                        pendingPhotoFile = file
                        takePicture.launch(uri)
                    },
                    onPickFromGallery = {
                        pickFromGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onPhotoClick = { previewPhoto = it },
                )
            }
            item { HorizontalDivider() }
            item {
                Text(
                    text = "Переписка",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }
            if (uiState.messages.isEmpty()) {
                item {
                    Text(
                        text = "Переписки пока нет. Сообщения сохраняются локально мгновенно, без интернета.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp),
                    )
                }
            } else {
                items(uiState.messages, key = { it.id }) { message ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        MessageBubble(message)
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Удалить заявку?") },
            text = { Text("Действие необратимо.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; viewModel.deleteOrder(onBack) }) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Отмена") }
            },
        )
    }

    previewPhoto?.let { photo ->
        PhotoPreviewDialog(
            photo = photo,
            onDismiss = { previewPhoto = null },
            onDelete = {
                viewModel.deletePhoto(photo)
                previewPhoto = null
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrderSummary(
    clientName: String,
    clientPhone: String?,
    clientAddress: String?,
    category: String,
    description: String?,
    materialsNote: String?,
    price: Double?,
    status: OrderStatus,
    masterName: String?,
    masters: List<MasterEntity>,
    startedAt: Long?,
    finishedAt: Long?,
    onStatusChange: (OrderStatus) -> Unit,
    onMasterChange: (String?) -> Unit,
) {
    var masterMenuExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = clientName, style = MaterialTheme.typography.titleMedium)
        clientPhone?.let {
            Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        clientAddress?.let {
            Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Text(
            text = category,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 10.dp),
        )

        description?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        materialsNote?.let {
            Column(modifier = Modifier.padding(top = 10.dp)) {
                Text(
                    text = "Материалы",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(text = it, style = MaterialTheme.typography.bodyMedium)
            }
        }
        price?.let {
            Text(
                text = "Стоимость: ${it.toInt()} ₽",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 10.dp),
            )
        }

        Text(
            text = "Статус",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 14.dp, bottom = 6.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(OrderStatus.NEW, OrderStatus.IN_PROGRESS, OrderStatus.DONE).forEach { status0 ->
                    FilterChip(
                        selected = status == status0,
                        onClick = { onStatusChange(status0) },
                        label = { Text(status0.label) },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = status == OrderStatus.CANCELLED,
                    onClick = { onStatusChange(OrderStatus.CANCELLED) },
                    label = { Text(OrderStatus.CANCELLED.label) },
                )
            }
        }

        Text(
            text = "Мастер",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 14.dp, bottom = 6.dp),
        )
        Box {
            TextButton(onClick = { masterMenuExpanded = true }) {
                Text(masterName ?: "Не назначен")
            }
            DropdownMenu(expanded = masterMenuExpanded, onDismissRequest = { masterMenuExpanded = false }) {
                DropdownMenuItem(text = { Text("Не назначен") }, onClick = {
                    masterMenuExpanded = false
                    onMasterChange(null)
                })
                masters.forEach { master ->
                    DropdownMenuItem(text = { Text(master.fullName) }, onClick = {
                        masterMenuExpanded = false
                        onMasterChange(master.id)
                    })
                }
            }
        }

        if (startedAt != null || finishedAt != null) {
            Column(modifier = Modifier.padding(top = 10.dp)) {
                startedAt?.let {
                    Text(
                        text = "Начало: ${formatDateTime(it)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                finishedAt?.let {
                    Text(
                        text = "Окончание: ${formatDateTime(it)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotosSection(
    photos: List<OrderPhotoEntity>,
    onAddPhoto: () -> Unit,
    onPickFromGallery: () -> Unit,
    onPhotoClick: (OrderPhotoEntity) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Фото", style = MaterialTheme.typography.titleMedium)
            Row {
                TextButton(onClick = onPickFromGallery) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Галерея")
                }
                TextButton(onClick = onAddPhoto) {
                    Icon(Icons.Filled.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Камера")
                }
            }
        }
        if (photos.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                items(photos, key = { it.id }) { photo ->
                    AsyncImage(
                        model = File(photo.filePath),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onPhotoClick(photo) },
                    )
                }
            }
        } else {
            Text(
                text = "Фото ещё нет — снимите, что сделано и как.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun PhotoPreviewDialog(photo: OrderPhotoEntity, onDismiss: () -> Unit, onDelete: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(12.dp)),
        ) {
            AsyncImage(
                model = File(photo.filePath),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(Icons.Filled.Close, contentDescription = "Закрыть", tint = MaterialTheme.colorScheme.onBackground)
            }
            IconButton(onClick = onDelete, modifier = Modifier.align(Alignment.TopStart)) {
                Icon(Icons.Filled.Delete, contentDescription = "Удалить фото", tint = MaterialTheme.colorScheme.onBackground)
            }
        }
    }
}

@Composable
private fun MessageBubble(message: MessageEntity) {
    val alignment = if (message.fromMaster) Alignment.End else Alignment.Start
    val bg = if (message.fromMaster) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (message.fromMaster) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(bg, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(text = message.text, color = fg, style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            text = formatDateTime(message.createdAt),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun MessageInputBar(value: String, onValueChange: (String) -> Unit, onSend: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Написать клиенту или мастеру...") },
        )
        Spacer(Modifier.padding(4.dp))
        IconButton(onClick = onSend) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Отправить")
        }
    }
}
