package com.paliapp.ecommerce.ui.admin

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                title = { Text("नए ऑर्डर") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "पीछे")
                    }
                },
                actions = {
                    IconButton(onClick = { orderVm.loadAllOrders() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "अपडेट")
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
            title = { Text("डिलीट करें") },
            text = { Text("क्या आप सच में ${selectedOrderIds.size} ऑर्डर डिलीट करना चाहते हैं? यह वापस नहीं आएगा।") },
            confirmButton = {
                Button(
                    onClick = {
                        val ordersToDelete = deliveredOrders.filter { it.id in selectedOrderIds }
                        orderVm.archiveAndDeleteOrders(ordersToDelete) { success ->
                            if (success) {
                                Toast.makeText(context, "${ordersToDelete.size} ऑर्डर डिलीट हो गए", Toast.LENGTH_SHORT).show()
                                isSelectionMode = false
                                selectedOrderIds = emptySet()
                            } else {
                                Toast.makeText(context, "डिलीट करने में विफल", Toast.LENGTH_SHORT).show()
                            }
                        }
                        showDeleteConfirmationDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("डिलीट करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmationDialog = false }) {
                    Text("रद्द करें")
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
                    Text(if (isSelectionMode) "${selectedOrderIds.size} चुने गए" else "डिलीवर हुए ऑर्डर")
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
                        Icon(if (isSelectionMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        IconButton(onClick = {
                            selectedOrderIds = if (selectedOrderIds.size == deliveredOrders.size) {
                                emptySet()
                            } else {
                                deliveredOrders.map { it.id }.toSet()
                            }
                        }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "सब चुनें")
                        }

                        IconButton(onClick = { showDeleteConfirmationDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "चुने हुए हटाएं", tint = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        IconButton(onClick = { orderVm.loadAllOrders() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "अपडेट")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReturnsScreen(
    onBack: () -> Unit,
    orderVm: OrderViewModel = viewModel()
) {
    val returnOrders = orderVm.orders.value.filter { 
        it.status in listOf("RETURN_REQUESTED", "RETURN_APPROVED", "REFUNDED", "RETURNED") 
    }
    
    var selectedOrderIds by remember { mutableStateOf(setOf<String>()) }
    var isSelectionMode by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }

    if (showDeleteConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmationDialog = false },
            title = { Text("डिलीट करें") },
            text = { Text("क्या आप सच में ${selectedOrderIds.size} वापसी ऑर्डर डिलीट करना चाहते हैं?") },
            confirmButton = {
                Button(
                    onClick = {
                        val ordersToDelete = returnOrders.filter { it.id in selectedOrderIds }
                        orderVm.archiveAndDeleteOrders(ordersToDelete) { success ->
                            if (success) {
                                Toast.makeText(context, "${ordersToDelete.size} ऑर्डर डिलीट हो गए", Toast.LENGTH_SHORT).show()
                                isSelectionMode = false
                                selectedOrderIds = emptySet()
                            } else {
                                Toast.makeText(context, "डिलीट करने में विफल", Toast.LENGTH_SHORT).show()
                            }
                        }
                        showDeleteConfirmationDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("डिलीट करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmationDialog = false }) {
                    Text("रद्द करें")
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
                    Text(if (isSelectionMode) "${selectedOrderIds.size} चुने गए" else "वापसी की गुज़ारिश") 
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
                        Icon(if (isSelectionMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        IconButton(onClick = {
                            selectedOrderIds = if (selectedOrderIds.size == returnOrders.size) {
                                emptySet()
                            } else {
                                returnOrders.map { it.id }.toSet()
                            }
                        }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "सब चुनें")
                        }

                        IconButton(onClick = { showDeleteConfirmationDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "हटाएं", tint = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        IconButton(onClick = { orderVm.loadAllOrders() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "अपडेट")
                        }
                    }
                }
            )
        }
    ) { padding ->
        OrderList(
            orders = returnOrders,
            orderVm = orderVm,
            isReturn = true,
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
                val order = returnOrders.find { it.id == orderId }
                if (order?.status == "RETURNED") {
                    isSelectionMode = true
                    selectedOrderIds = selectedOrderIds + orderId
                }
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
    isReturn: Boolean = false,
    isSelectionMode: Boolean = false,
    selectedOrderIds: Set<String> = emptySet(),
    onOrderSelect: (String) -> Unit = {},
    onOrderLongClick: (String) -> Unit = {}
) {
    if (orders.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("कोई ऑर्डर नहीं मिला", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { orderVm.loadAllOrders() }) {
                    Text("अपडेट करें")
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
                    isReturn = isReturn,
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
    isReturn: Boolean = false,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    var showDateDialog by remember { mutableStateOf(false) }
    var deliveryDateInput by remember { mutableStateOf(order.deliveryDate) }
    val context = LocalContext.current
    var showSingleDeleteConfirmationDialog by remember { mutableStateOf(false) }
    
    var showExamineDialog by remember { mutableStateOf(false) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var adminNote by remember { mutableStateOf(order.returnAdminNote) }

    if (showSingleDeleteConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showSingleDeleteConfirmationDialog = false },
            title = { Text("डिलीट करें") },
            text = { Text("क्या आप इस ऑर्डर को डिलीट करना चाहते हैं? यह आर्काइव में चला जाएगा।") },
            confirmButton = {
                Button(
                    onClick = {
                        orderVm.archiveAndDeleteOrders(listOf(order)) { success ->
                            if (success) Toast.makeText(context, "ऑर्डर आर्काइव हुआ", Toast.LENGTH_SHORT).show()
                        }
                        showSingleDeleteConfirmationDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("डिलीट करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSingleDeleteConfirmationDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isDelivered || (isReturn && order.status == "RETURNED")) {
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
                            text = "आर्डर नंबर: ${order.id.takeLast(6)}",
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
                        Icon(Icons.Default.Download, contentDescription = "बिल डाउनलोड")
                    }
                    if ((isDelivered || (isReturn && order.status == "RETURNED")) && !isSelectionMode) {
                        IconButton(onClick = { showSingleDeleteConfirmationDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "आर्काइव करें", tint = MaterialTheme.colorScheme.error)
                        }
                    } else if (!isDelivered && !isReturn) {
                        StatusBadge(status = order.status)
                    } else if (isReturn) {
                        StatusBadge(status = order.status)
                    }
                }
            }

            if (isReturn) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFBE9E7).copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(text = "ग्राहक का कारण:", style = MaterialTheme.typography.labelMedium, color = Color(0xFFD84315), fontWeight = FontWeight.Bold)
                        Text(text = order.returnReason.ifEmpty { "कोई कारण नहीं दिया" }, style = MaterialTheme.typography.bodyMedium)
                        
                        if (order.returnAdminNote.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "एडमिन नोट:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text(text = order.returnAdminNote, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "डिलीवरी की तारीख:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = order.deliveryDate.ifEmpty { "सेट नहीं है" },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (!isDelivered && !isReturn && order.status != "CANCELLED") {
                    IconButton(onClick = { showDateDialog = true }) {
                        Icon(Icons.Default.EditCalendar, contentDescription = "तारीख चुनें")
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Text(text = "ग्राहक की जानकारी:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
            Text(text = order.userName.ifEmpty { "N/A" }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(text = order.userMobile.ifEmpty { "N/A" }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = order.address.ifEmpty { "पता नहीं दिया" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Text(text = "सामान:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                order.items.forEach { item ->
                    val isPartOfReturn = isReturn && (item.status == "RETURN_REQUESTED" || item.status == "RETURN_APPROVED" || item.status == "REFUNDED" || item.status == "RETURNED")
                    
                    Surface(
                        color = if (isPartOfReturn) Color(0xFFFFF3E0).copy(alpha = 0.6f) else Color.Transparent,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(if (isPartOfReturn) 8.dp else 0.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    fontWeight = if (isPartOfReturn) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = if (isPartOfReturn) "वापसी: ${item.returnQty} / ${item.qty}" else "खरीदा: ${item.qty}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isPartOfReturn) Color(0xFFD84315) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "₹${item.price * (if (isPartOfReturn) item.returnQty else item.qty)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (!item.isReturnable) {
                                    Text(
                                        text = "वापस नहीं होगा",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else if (isReturn && !isPartOfReturn) {
                                    val statusHindi = when(item.status) {
                                        "DELIVERED" -> "पहुंच गया"
                                        else -> item.status.replace("_", " ")
                                    }
                                    Text(
                                        text = statusHindi,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                } else if (item.status == "RETURN_REJECTED") {
                                    Text(
                                        text = "वापसी अस्वीकार",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Red,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "कुल रकम", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = "₹${order.totalAmount}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            }

            if (isReturn) {
                val refundAmount = order.items.filter { it.status in listOf("RETURN_APPROVED", "REFUNDED", "RETURNED", "RETURN_REQUESTED") }.sumOf { it.price * it.returnQty }
                
                if (refundAmount > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "वापसी रकम (Refund):",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD84315)
                            )
                            Text(
                                text = "₹$refundAmount",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFD84315),
                                fontSize = 24.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                when (order.status) {
                    "RETURN_REQUESTED" -> {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showExamineDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("जांचें और मंजूर करें")
                            }
                            OutlinedButton(
                                onClick = { showRejectDialog = true },
                                modifier = Modifier.weight(0.6f)
                            ) {
                                Text("मना करें")
                            }
                        }
                    }
                    "RETURN_APPROVED" -> {
                        Button(
                            onClick = { orderVm.markAsRefunded(order) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7))
                        ) {
                            Icon(Icons.Default.Payments, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("पैसे लौटा दिए (Refunded)")
                        }
                    }
                    "REFUNDED" -> {
                        Button(
                            onClick = { orderVm.updateOrderStatus(order.id, "RETURNED") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                        ) {
                            Text("वापसी पूरी करें")
                        }
                    }
                }
            } else if (order.status == "PLACED") {
                Surface(
                    color = when (order.paymentStatus) {
                        "PAID" -> Color(0xFFE8F5E9)
                        "AWAITING_APPROVAL" -> Color(0xFFFFF3E0)
                        else -> Color(0xFFEEEEEE)
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val paymentHindi = when(order.paymentStatus) {
                                "PAID" -> "पैसे मिल गए"
                                "AWAITING_APPROVAL" -> "अप्रूवल का इंतज़ार"
                                "PENDING" -> "बाकी है"
                                else -> order.paymentStatus
                            }
                            Text(
                                text = "पेमेंट स्टेटस: $paymentHindi",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = when (order.paymentStatus) {
                                    "PAID" -> Color(0xFF2E7D32)
                                    "AWAITING_APPROVAL" -> Color(0xFFE65100)
                                    else -> Color.DarkGray
                                }
                            )
                            if (order.paymentMethod.isNotEmpty()) {
                                Text(
                                    text = " (${order.paymentMethod})",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        
                        if (order.paymentStatus != "PAID") {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { orderVm.updatePaymentStatus(order.id, "PAID") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("पैसे मिल गए (Mark as PAID)")
                            }
                        }
                    }
                }

                Button(
                    onClick = { orderVm.updateOrderStatus(order.id, "DELIVERED") },
                    enabled = order.paymentStatus == "PAID",
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(if (order.paymentStatus == "PAID") "डिलीवर हो गया" else "पेमेंट का इंतज़ार करें")
                }
            }
        }
    }

    if (showExamineDialog) {
        AlertDialog(
            onDismissRequest = { showExamineDialog = false },
            title = { Text("सामान की जांच") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("क्या सामान वापस लेने लायक है?")
                    OutlinedTextField(
                        value = adminNote,
                        onValueChange = { adminNote = it },
                        label = { Text("एडमिन नोट (जरूरी नहीं)") },
                        placeholder = { Text("जैसे: सामान सही हालत में मिला।") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    orderVm.approveReturn(order, adminNote)
                    showExamineDialog = false
                }) { Text("मंजूर करें") }
            },
            dismissButton = {
                TextButton(onClick = { showExamineDialog = false }) { Text("रद्द करें") }
            }
        )
    }

    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("वापसी रद्द करें") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("क्या आप सच में यह वापसी रद्द करना चाहते हैं?")
                    OutlinedTextField(
                        value = adminNote,
                        onValueChange = { adminNote = it },
                        label = { Text("कारण (जरूरी नहीं)") },
                        placeholder = { Text("जैसे: सामान ग्राहक ने खराब किया है।") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        orderVm.rejectReturn(order, adminNote)
                        showRejectDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("रद्द करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) { Text("वापस") }
            }
        )
    }

    if (showDateDialog) {
        AlertDialog(
            onDismissRequest = { showDateDialog = false },
            title = { Text("डिलीवरी की तारीख") },
            text = {
                OutlinedTextField(
                    value = deliveryDateInput,
                    onValueChange = { deliveryDateInput = it },
                    label = { Text("डिलीवरी कब होगी?") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    orderVm.updateDeliveryDate(order.id, deliveryDateInput)
                    showDateDialog = false
                }) { Text("अपडेट करें") }
            },
            dismissButton = {
                TextButton(onClick = { showDateDialog = false }) { Text("रद्द करें") }
            }
        )
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (color, bgColor) = when (status) {
        "PLACED" -> Color(0xFF1976D2) to Color(0xFFE3F2FD)
        "DELIVERED" -> Color(0xFF388E3C) to Color(0xFFE8F5E9)
        "RETURN_REQUESTED" -> Color(0xFFE65100) to Color(0xFFFFF3E0)
        "RETURN_APPROVED" -> Color(0xFF0277BD) to Color(0xFFE1F5FE)
        "REFUNDED" -> Color(0xFF673AB7) to Color(0xFFEDE7F6)
        "RETURNED" -> Color(0xFF2E7D32) to Color(0xFFE8F5E9)
        "CANCELLED" -> Color(0xFFD32F2F) to Color(0xFFFFEBEE)
        "RETURN_REJECTED" -> Color(0xFFD32F2F) to Color(0xFFFFEBEE)
        else -> Color.Gray to Color.LightGray
    }
    
    Surface(
        color = bgColor,
        shape = MaterialTheme.shapes.small
    ) {
        val statusHindi = when(status) {
            "PLACED" -> "नया ऑर्डर"
            "DELIVERED" -> "पहुंच गया"
            "RETURN_REQUESTED" -> "वापसी गुज़ारिश"
            "RETURN_APPROVED" -> "वापसी मंजूर"
            "REFUNDED" -> "पैसे लौटाए"
            "RETURNED" -> "वापस हुआ"
            "CANCELLED" -> "रद्द हुआ"
            "RETURN_REJECTED" -> "वापसी रद्द"
            else -> status.replace("_", " ")
        }
        Text(
            text = statusHindi,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelLarge,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}
