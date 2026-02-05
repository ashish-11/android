package com.paliapp.ecommerce.ui.customer

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.paliapp.ecommerce.data.model.CartItem
import com.paliapp.ecommerce.data.model.Order
import com.paliapp.ecommerce.utils.BillGenerator
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
    var orderForReturn by remember { mutableStateOf<Order?>(null) }
    val context = LocalContext.current

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            orderVm.loadCustomerOrders(userId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("मेरे आर्डर") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "पीछे")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        if (userId.isNotEmpty()) orderVm.loadCustomerOrders(userId) 
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "अपडेट")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (orders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("आपने अभी तक कोई आर्डर नहीं दिया है।")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { if (userId.isNotEmpty()) orderVm.loadCustomerOrders(userId) }) {
                        Text("अपडेट करें")
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
                    OrderCard(
                        order = order, 
                        onPay = { orderToPay = order },
                        onRequestReturn = { orderForReturn = order }
                    )
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

    if (orderForReturn != null) {
        PartialReturnDialog(
            order = orderForReturn!!,
            onDismiss = { orderForReturn = null },
            onConfirm = { reason, updatedItems ->
                orderVm.requestPartialReturn(orderForReturn!!.id, reason, updatedItems) { success ->
                    if (success) {
                        Toast.makeText(context, "वापसी (Return) की गुज़ारिश भेज दी गई है", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "गुज़ारिश भेजने में दिक्कत हुई", Toast.LENGTH_SHORT).show()
                    }
                }
                orderForReturn = null
            }
        )
    }
}

@Composable
fun OrderCard(order: Order, onPay: () -> Unit, onRequestReturn: () -> Unit) {
    val context = LocalContext.current
    val canRequestReturn = order.status == "DELIVERED" && order.items.any { it.isReturnable && it.status == "DELIVERED" }
    val isPartiallyReturned = order.status == "RETURNED" && order.items.any { it.status == "DELIVERED" }
    
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
                    text = "आर्डर नंबर: ${order.id.takeLast(6)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { BillGenerator.downloadBill(context, order) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Download, contentDescription = "बिल डाउनलोड", modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    StatusBadge(status = if (isPartiallyReturned) "PARTIAL_RETURNED" else order.status)
                }
            }
            
            Text(
                text = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                    .format(Date(order.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (order.deliveryDate.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "डिलीवरी की तारीख: ",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = order.deliveryDate,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Text(text = "सामान का विवरण", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
            order.items.forEach { item ->
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${item.name} x ${item.qty}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (item.isReturnable) {
                                Text(
                                    text = "${item.returnWindowDays} दिनों में वापस कर सकते हैं",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF388E3C)
                                )
                            } else {
                                Text(
                                    text = "वापस नहीं होगा",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                            }
                        }
                        Text(
                            text = "₹${item.price * item.qty}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            val returnedItems = order.items.filter { it.returnQty > 0 }
            if (returnedItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "वापसी की जानकारी", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFFD84315))
                returnedItems.forEach { item ->
                    val statusColor = when(item.status) {
                        "RETURN_REJECTED" -> Color.Red
                        "RETURNED", "REFUNDED" -> Color(0xFF388E3C)
                        else -> Color(0xFFE65100)
                    }
                    val bgStatusColor = statusColor.copy(alpha = 0.1f)

                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = "${item.name} (वापसी Qty: ${item.returnQty})",
                            style = MaterialTheme.typography.bodySmall,
                            color = statusColor
                        )
                        Surface(color = bgStatusColor, shape = RoundedCornerShape(4.dp)) {
                            val hindiStatus = when(item.status) {
                                "RETURNED" -> "वापस हो गया"
                                "REFUNDED" -> "पैसे लौटा दिए"
                                "RETURN_REJECTED" -> "वापसी रद्द"
                                "RETURN_REQUESTED" -> "गुज़ारिश भेजी है"
                                else -> item.status
                            }
                            Text(
                                text = hindiStatus,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "कुल रकम", style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = "₹${order.totalAmount}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    when {
                        order.status == "RETURN_REQUESTED" -> {
                            Text("वापसी की गुज़ारिश", color = Color(0xFFE65100), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                        order.status == "RETURNED" -> {
                            Text(if (isPartiallyReturned) "कुछ सामान वापस" else "वापस हो गया", color = Color(0xFF2E7D32), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                        order.status == "CANCELLED" -> {
                            Text("रद्द किया गया", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                        order.paymentStatus == "PENDING" && order.status == "PLACED" -> {
                            Button(onClick = onPay) {
                                Text(if (order.paymentMethod.isEmpty()) "पैसे भरें" else "पेमेंट बदलें")
                            }
                        }
                        canRequestReturn -> {
                            OutlinedButton(onClick = onRequestReturn) {
                                Text("वापस करें")
                            }
                        }
                        else -> {
                            val paymentText = if (order.paymentStatus == "PAID") "पैसे मिल गए" else "पेमेंट: ${order.paymentStatus}"
                            val paymentColor = if (order.paymentStatus == "PAID") Color(0xFF2E7D32) else Color(0xFFE65100)
                            Text(paymentText, color = paymentColor, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PartialReturnDialog(
    order: Order,
    onDismiss: () -> Unit,
    onConfirm: (String, List<CartItem>) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    val returnableItems = order.items.filter { it.isReturnable && it.status == "DELIVERED" }
    val returnQuantities = remember { 
        mutableStateMapOf<String, Int>().apply {
            returnableItems.forEach { put(it.id, 0) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("सामान वापस करें") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Text("जो सामान वापस करना है उसकी मात्रा (Qty) चुनें।", style = MaterialTheme.typography.bodyMedium)
                }
                
                items(returnableItems) { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = item.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(text = "खरीदा हुआ: ${item.qty}", style = MaterialTheme.typography.labelSmall)
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { 
                                    val current = returnQuantities[item.id] ?: 0
                                    if (current > 0) returnQuantities[item.id] = current - 1
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = null)
                            }
                            
                            Text(
                                text = (returnQuantities[item.id] ?: 0).toString(),
                                modifier = Modifier.padding(horizontal = 8.dp),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            
                            IconButton(
                                onClick = { 
                                    val current = returnQuantities[item.id] ?: 0
                                    if (current < item.qty) returnQuantities[item.id] = current + 1
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                            }
                        }
                    }
                }
                
                item {
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("वापसी का कारण (जरूरी नहीं)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            }
        },
        confirmButton = {
            val totalToReturn = returnQuantities.values.sum()
            Button(
                onClick = {
                    val updatedItems = order.items.map { item ->
                        val rQty = returnQuantities[item.id] ?: 0
                        if (rQty > 0) {
                            item.copy(
                                status = "RETURN_REQUESTED",
                                returnQty = rQty
                            ) 
                        } else {
                            item
                        }
                    }
                    onConfirm(reason, updatedItems)
                },
                enabled = totalToReturn > 0
            ) {
                Text("गुज़ारिश भेजें")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("रद्द करें") }
        }
    )
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
        title = { Text("पेमेंट का तरीका चुनें") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("कुल रकम: ₹${order.totalAmount}", fontWeight = FontWeight.Bold)
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selectedMethod == "COD", onClick = { selectedMethod = "COD" })
                    Text("डिलीवरी पर नकद (Cash on Delivery)")
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selectedMethod == "UPI", onClick = { selectedMethod = "UPI" })
                    Text("UPI (QR कोड)")
                }

                if (selectedMethod == "UPI") {
                    if (qrUrl.isNotEmpty()) {
                        AsyncImage(
                            model = getDirectImageUrl(qrUrl),
                            contentDescription = "UPI QR कोड",
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text("QR कोड अभी उपलब्ध नहीं है। कृपया दुकानदार से बात करें।", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSelect(selectedMethod) },
                enabled = selectedMethod.isNotEmpty()
            ) {
                Text("पक्का करें")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("रद्द करें") }
        }
    )
}

private fun getDirectImageUrl(url: String): String {
    return if (url.contains("drive.google.com")) {
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
    val color = when (status) {
        "PLACED" -> Color(0xFF1976D2)
        "DELIVERED" -> Color(0xFF388E3C)
        "RETURN_REQUESTED" -> Color(0xFFE65100)
        "RETURNED" -> Color(0xFF388E3C)
        "PARTIAL_RETURNED" -> Color(0xFF2E7D32)
        "CANCELLED" -> Color(0xFFD32F2F)
        "RETURN_REJECTED" -> Color(0xFFD32F2F)
        else -> Color.Gray
    }
    val bgColor = color.copy(alpha = 0.1f)
    
    Surface(
        color = bgColor,
        shape = MaterialTheme.shapes.small
    ) {
        val text = when(status) {
            "PLACED" -> "ऑर्डर मिला"
            "DELIVERED" -> "मिल गया"
            "RETURN_REQUESTED" -> "वापसी की गुज़ारिश"
            "RETURNED" -> "वापस हो गया"
            "PARTIAL_RETURNED" -> "कुछ सामान वापस"
            "CANCELLED" -> "रद्द हुआ"
            "RETURN_REJECTED" -> "वापसी रद्द"
            else -> status.replace("_", " ")
        }
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
