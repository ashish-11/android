package com.paliapp.ecommerce.ui.admin

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paliapp.ecommerce.data.model.Order
import com.paliapp.ecommerce.utils.BillGenerator
import com.paliapp.ecommerce.viewmodel.OrderViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOrdersScreen(
    onBack: () -> Unit,
    orderVm: OrderViewModel = viewModel()
) {
    val pendingOrders = orderVm.orders.value.filter { it.status == "PLACED" }

    LaunchedEffect(Unit) {
        orderVm.loadAllOrders()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pending Orders") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { orderVm.loadAllOrders() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        OrderList(
            orders = pendingOrders,
            orderVm = orderVm,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDeliveredOrdersScreen(
    onBack: () -> Unit,
    orderVm: OrderViewModel = viewModel()
) {
    val deliveredOrders = orderVm.orders.value.filter { it.status == "DELIVERED" }
    var selectedOrderIds by remember { mutableStateOf(setOf<String>()) }
    var isSelectionMode by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }

    if (showDeleteConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmationDialog = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete ${selectedOrderIds.size} selected order(s)? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        val ordersToDelete = deliveredOrders.filter { it.id in selectedOrderIds }
                        orderVm.archiveAndDeleteOrders(ordersToDelete) { success ->
                            if (success) {
                                Toast.makeText(context, "Successfully deleted ${ordersToDelete.size} orders", Toast.LENGTH_SHORT).show()
                                isSelectionMode = false
                                selectedOrderIds = emptySet()
                            } else {
                                Toast.makeText(context, "Failed to delete orders", Toast.LENGTH_SHORT).show()
                            }
                        }
                        showDeleteConfirmationDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        orderVm.loadAllOrders()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (isSelectionMode) "${selectedOrderIds.size} Selected" else "Delivered Orders")
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSelectionMode) {
                            isSelectionMode = false
                            selectedOrderIds = emptySet()
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(if (isSelectionMode) Icons.Default.Close else Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        IconButton(onClick = {
                            if (selectedOrderIds.size == deliveredOrders.size) {
                                selectedOrderIds = emptySet()
                            } else {
                                selectedOrderIds = deliveredOrders.map { it.id }.toSet()
                            }
                        }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                        }

                        IconButton(onClick = { showDeleteConfirmationDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected", tint = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        IconButton(onClick = { orderVm.loadAllOrders() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                }
            )
        }
    ) { padding ->
        OrderList(
            orders = deliveredOrders,
            orderVm = orderVm,
            isDelivered = true,
            isSelectionMode = isSelectionMode,
            selectedOrderIds = selectedOrderIds,
            onOrderSelect = { orderId ->
                selectedOrderIds = if (selectedOrderIds.contains(orderId)) {
                    val newSet = selectedOrderIds - orderId
                    if (newSet.isEmpty()) isSelectionMode = false
                    newSet
                } else {
                    selectedOrderIds + orderId
                }
            },
            onOrderLongClick = { orderId ->
                isSelectionMode = true
                selectedOrderIds = selectedOrderIds + orderId
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        )
    }
}

@Composable
private fun OrderList(
    orders: List<Order>,
    orderVm: OrderViewModel,
    modifier: Modifier = Modifier,
    isDelivered: Boolean = false,
    isSelectionMode: Boolean = false,
    selectedOrderIds: Set<String> = emptySet(),
    onOrderSelect: (String) -> Unit = {},
    onOrderLongClick: (String) -> Unit = {}
) {
    if (orders.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No orders found", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { orderVm.loadAllOrders() }) {
                    Text("Refresh")
                }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(orders, key = { it.id }) { order ->
                OrderCard(
                    order = order,
                    orderVm = orderVm,
                    isDelivered = isDelivered,
                    isSelected = selectedOrderIds.contains(order.id),
                    isSelectionMode = isSelectionMode,
                    onClick = {
                        if (isSelectionMode) onOrderSelect(order.id)
                    },
                    onLongClick = {
                        if (!isSelectionMode) onOrderLongClick(order.id)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OrderCard(
    order: Order,
    orderVm: OrderViewModel,
    isDelivered: Boolean,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    var showDateDialog by remember { mutableStateOf(false) }
    var deliveryDateInput by remember { mutableStateOf(order.deliveryDate) }
    val context = LocalContext.current
    var showSingleDeleteConfirmationDialog by remember { mutableStateOf(false) }

    if (showSingleDeleteConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showSingleDeleteConfirmationDialog = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete this order? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        orderVm.archiveAndDeleteOrders(listOf(order)) { success ->
                            if (success) Toast.makeText(context, "Order deleted", Toast.LENGTH_SHORT).show()
                        }
                        showSingleDeleteConfirmationDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSingleDeleteConfirmationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isDelivered) {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                } else Modifier
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = if (isSelected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSelectionMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onClick() },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Order ID: ${order.id.takeLast(6)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                                .format(Date(order.timestamp)),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { BillGenerator.downloadBill(context, order) }) {
                        Icon(Icons.Default.Download, contentDescription = "Download Bill")
                    }
                    if (isDelivered && !isSelectionMode) {
                        IconButton(onClick = { showSingleDeleteConfirmationDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Order", tint = MaterialTheme.colorScheme.error)
                        }
                    } else if (!isDelivered) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = order.status,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // Delivery Date Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Delivery Date:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = order.deliveryDate.ifEmpty { "Not set" },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (!isDelivered && order.status != "CANCELLED") {
                    IconButton(onClick = { showDateDialog = true }) {
                        Icon(Icons.Default.EditCalendar, contentDescription = "Set Date")
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // Payment Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Payment: ${order.paymentMethod.ifEmpty { "Not selected" }}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = order.paymentStatus,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = when (order.paymentStatus) {
                            "PAID" -> Color(0xFF2E7D32)
                            "AWAITING_APPROVAL" -> Color(0xFFE65100)
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                }

                if (order.paymentStatus != "PAID") {
                    Button(
                        onClick = { orderVm.updatePaymentStatus(order.id, "PAID") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Text("Approve Payment")
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            Text(
                text = "Customer Details:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = order.userName.ifEmpty { "N/A" },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )

            // Display Customer Email
            if (order.userEmail.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = order.userEmail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = order.userMobile.ifEmpty { "N/A" }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp).padding(top = 2.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = order.address.ifEmpty { "No address provided" }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            Text(text = "Items:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
            order.items.forEach { item ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "${item.name} x ${item.qty}", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "₹${item.price * item.qty}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Total Amount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = "₹${order.totalAmount}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            }

            if (order.status == "PLACED") {
                Button(
                    onClick = { orderVm.updateOrderStatus(order.id, "DELIVERED") },
                    enabled = order.paymentStatus == "PAID",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(if (order.paymentStatus == "PAID") "Mark as Delivered" else "Wait for Payment")
                }
            }
        }
    }

    if (showDateDialog) {
        AlertDialog(
            onDismissRequest = { showDateDialog = false },
            title = { Text("Set Delivery Date") },
            text = {
                OutlinedTextField(
                    value = deliveryDateInput,
                    onValueChange = { deliveryDateInput = it },
                    label = { Text("Expected Delivery Date") },
                    placeholder = { Text("e.g. 25 Oct, Morning") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    orderVm.updateDeliveryDate(order.id, deliveryDateInput)
                    showDateDialog = false
                }) { Text("Update") }
            },
            dismissButton = {
                TextButton(onClick = { showDateDialog = false }) { Text("Cancel") }
            }
        )
    }
}
