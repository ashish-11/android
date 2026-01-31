package com.paliapp.ecommerce.ui.customer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.paliapp.ecommerce.data.model.Order
import com.paliapp.ecommerce.viewmodel.OrderViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerOrdersScreen(
    onBack: () -> Unit,
    orderVm: OrderViewModel = viewModel()
) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val orders = orderVm.customerOrders.value
    var orderToPay by remember { mutableStateOf<Order?>(null) }

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            orderVm.loadCustomerOrders(userId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Orders") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        if (userId.isNotEmpty()) orderVm.loadCustomerOrders(userId) 
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (orders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("You haven't placed any orders yet.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { if (userId.isNotEmpty()) orderVm.loadCustomerOrders(userId) }) {
                        Text("Refresh")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(orders) { order ->
                    OrderCard(order = order, onPay = { orderToPay = order })
                }
            }
        }
    }

    if (orderToPay != null) {
        PaymentMethodDialog(
            order = orderToPay!!,
            qrUrl = orderVm.upiQrUrl.value ?: "",
            onDismiss = { orderToPay = null },
            onSelect = { method ->
                orderVm.setPaymentMethod(orderToPay!!.id, method)
                orderToPay = null
            }
        )
    }
}

@Composable
fun OrderCard(order: Order, onPay: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Order ID: ${order.id.takeLast(6)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                StatusBadge(status = order.status)
            }
            
            Text(
                text = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                    .format(Date(order.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (order.deliveryDate.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        text = "Expected Delivery: ${order.deliveryDate}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            order.items.forEach { item ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = "${item.name} x ${item.qty}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "₹${item.price * item.qty}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Amount",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "₹${order.totalAmount}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                when {
                    order.status == "CANCELLED" -> {
                        Text("CANCELLED", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                    order.paymentStatus == "PAID" -> {
                        Text("PAID", color = Color(0xFF2E7D32), fontWeight = FontWeight.ExtraBold)
                    }
                    order.paymentStatus == "AWAITING_APPROVAL" -> {
                        Surface(color = Color(0xFFFFF3E0), shape = MaterialTheme.shapes.small) {
                            Text(
                                "Awaiting Admin Approval",
                                modifier = Modifier.padding(8.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFE65100),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    else -> {
                        Button(onClick = onPay) {
                            Text(if (order.paymentMethod.isEmpty()) "Pay Now" else "Change Payment")
                        }
                    }
                }
            }
            
            if (order.paymentMethod.isNotEmpty()) {
                Text(
                    text = "Method: ${order.paymentMethod}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun PaymentMethodDialog(
    order: Order,
    qrUrl: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    var selectedMethod by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Payment Method") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Total Amount: ₹${order.totalAmount}", fontWeight = FontWeight.Bold)
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selectedMethod == "COD", onClick = { selectedMethod = "COD" })
                    Text("Cash on Delivery")
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selectedMethod == "UPI", onClick = { selectedMethod = "UPI" })
                    Text("UPI (QR Code)")
                }

                if (selectedMethod == "UPI") {
                    if (qrUrl.isNotEmpty()) {
                        AsyncImage(
                            model = getDirectImageUrl(qrUrl),
                            contentDescription = "UPI QR Code",
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text("QR Code not available. Please contact admin.", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSelect(selectedMethod) },
                enabled = selectedMethod.isNotEmpty()
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Converts a Google Drive share link to a direct image URL.
 */
private fun getDirectImageUrl(url: String): String {
    return if (url.contains("drive.google.com")) {
        // Handle links like: https://drive.google.com/file/d/FILE_ID/view?usp=sharing
        val fileId = if (url.contains("/d/")) {
            url.substringAfter("/d/").substringBefore("/")
        } else if (url.contains("id=")) {
            url.substringAfter("id=").substringBefore("&")
        } else {
            ""
        }
        
        if (fileId.isNotEmpty()) {
            "https://drive.google.com/uc?export=view&id=$fileId"
        } else {
            url
        }
    } else {
        url
    }
}

@Composable
private fun StatusBadge(status: String) {
    val color = if (status == "PLACED") Color(0xFF1976D2) else Color(0xFF388E3C)
    val bgColor = if (status == "PLACED") Color(0xFFE3F2FD) else Color(0xFFE8F5E9)
    
    Surface(
        color = bgColor,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
