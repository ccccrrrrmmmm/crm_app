package ru.greenland.crm.ui.orders

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import ru.greenland.crm.data.local.entity.OrderStatus
import ru.greenland.crm.ui.components.StatusBadge

@Composable
fun OrdersListScreen(
    onOrderClick: (String) -> Unit,
    onAddClick: () -> Unit,
    viewModel: OrdersListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Filled.Add, contentDescription = "Новая заявка")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            StatusFilterRow(
                selected = uiState.filter,
                statusOrder = uiState.statusOrder,
                onSelect = viewModel::setFilter,
                onReorder = viewModel::setStatusOrder,
            )
            if (uiState.items.isEmpty()) {
                EmptyOrders()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(uiState.items, key = { it.order.id }) { item ->
                        OrderCard(item = item, onClick = { onOrderClick(item.order.id) })
                    }
                }
            }
        }
    }
}

/**
 * Строка фильтров-вкладок. «Все» закреплён первым и не двигается; вкладки-статусы можно
 * переставлять перетаскиванием (долгое нажатие → тащим влево/вправо), новый порядок сохраняется.
 */
@Composable
private fun StatusFilterRow(
    selected: OrderStatus?,
    statusOrder: List<OrderStatus>,
    onSelect: (OrderStatus?) -> Unit,
    onReorder: (List<OrderStatus>) -> Unit,
) {
    val density = LocalDensity.current
    val spacingPx = with(density) { 8.dp.toPx() }

    // Рабочая копия порядка: во время перетаскивания меняем её на лету, а извне (из VM)
    // подхватываем новый список только когда пользователь не тащит чип.
    val working = remember { mutableStateListOf<OrderStatus>().apply { addAll(statusOrder) } }
    var draggedStatus by remember { mutableStateOf<OrderStatus?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val widthByStatus = remember { mutableStateMapOf<OrderStatus, Int>() }

    LaunchedEffect(statusOrder) {
        if (draggedStatus == null) {
            working.clear()
            working.addAll(statusOrder)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text("Все") })

        working.forEach { status ->
            key(status) {
                val isDragging = draggedStatus == status
                FilterChip(
                    selected = selected == status,
                    onClick = { onSelect(status) },
                    label = { Text(status.label) },
                    modifier = Modifier
                        .onSizeChanged { widthByStatus[status] = it.width }
                        // Соседи плавно расступаются при перестановке; сам перетаскиваемый
                        // чип едет за пальцем через graphicsLayer, ему анимация не нужна.
                        .then(if (isDragging) Modifier else Modifier.animatePlacement())
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer {
                            translationX = if (isDragging) dragOffset else 0f
                            shadowElevation = if (isDragging) 8f else 0f
                        }
                        .pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggedStatus = status
                                    dragOffset = 0f
                                },
                                onDragEnd = {
                                    draggedStatus = null
                                    dragOffset = 0f
                                    onReorder(working.toList())
                                },
                                onDragCancel = {
                                    draggedStatus = null
                                    dragOffset = 0f
                                    onReorder(working.toList())
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffset += dragAmount.x
                                    val index = working.indexOf(status)
                                    if (dragOffset < 0 && index > 0) {
                                        val leftWidth = (widthByStatus[working[index - 1]] ?: 0).toFloat()
                                        if (-dragOffset > leftWidth / 2f + spacingPx / 2f) {
                                            working.removeAt(index)
                                            working.add(index - 1, status)
                                            dragOffset += leftWidth + spacingPx
                                        }
                                    } else if (dragOffset > 0 && index < working.size - 1) {
                                        val rightWidth = (widthByStatus[working[index + 1]] ?: 0).toFloat()
                                        if (dragOffset > rightWidth / 2f + spacingPx / 2f) {
                                            working.removeAt(index)
                                            working.add(index + 1, status)
                                            dragOffset -= rightWidth + spacingPx
                                        }
                                    }
                                },
                            )
                        },
                )
            }
        }
    }
}

/**
 * Плавно доводит элемент на новое место, когда его позиция в родителе меняется (например,
 * соседний чип переставили), вместо резкого прыжка. Не влияет на измерение — двигает только
 * при отрисовке через анимируемый offset.
 */
private fun Modifier.animatePlacement(): Modifier = composed {
    val scope = rememberCoroutineScope()
    var targetOffset by remember { mutableStateOf(IntOffset.Zero) }
    var animatable by remember { mutableStateOf<Animatable<IntOffset, AnimationVector2D>?>(null) }
    this
        .onPlaced { coordinates -> targetOffset = coordinates.positionInParent().round() }
        .offset {
            val anim = animatable
                ?: Animatable(targetOffset, IntOffset.VectorConverter).also { animatable = it }
            if (anim.targetValue != targetOffset) {
                scope.launch { anim.animateTo(targetOffset, spring(stiffness = Spring.StiffnessMediumLow)) }
            }
            anim.value - targetOffset
        }
}

@Composable
private fun OrderCard(item: OrderListItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = item.order.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.clientName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.order.category,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
            Box(modifier = Modifier.padding(top = 10.dp)) {
                StatusBadge(status = item.order.status)
            }
            item.masterName?.let {
                Text(
                    text = "Мастер: $it",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun EmptyOrders() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Заявок пока нет", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Нажмите + чтобы создать первую заявку",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
