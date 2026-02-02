package com.paliapp.ecommerce.ui.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paliapp.ecommerce.viewmodel.CartViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    cartVm: CartViewModel = viewModel(),
    onBack: () -> Unit,
    onOrderPlaced: () -> Unit
) {
    var address by remember { mutableStateOf("") }
    val savedAddress = cartVm.userAddress.value
    val cartItems = cartVm.cartItems.value
    val totalAmount = cartItems.sumOf { it.price * it.qty }
    var isPlacingOrder by remember { mutableStateOf(false) }
    var addressError by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Initialize address with saved address once it's loaded
    LaunchedEffect(savedAddress) {
        if (address.isEmpty() && savedAddress.isNotEmpty()) {
            address = savedAddress
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Checkout") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (cartItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Your cart is empty", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onBack) {
                            Text("Start Shopping")
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // List of items - naturally scrollable
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(cartItems) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = item.name.take(1).uppercase(),
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "₹${item.price} x ${item.qty}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    IconButton(onClick = { cartVm.removeFromCart(item.id) }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Remove",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Checkout section - Made scrollable itself if content exceeds space
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            OutlinedTextField(
                                value = address,
                                onValueChange = { 
                                    address = it 
                                    if (it.isNotBlank()) addressError = false
                                },
                                label = { Text("Full Delivery Address") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isPlacingOrder,
                                isError = addressError,
                                supportingText = {
                                    if (addressError) {
                                        Text("Address is required to place an order", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Total Amount",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "₹$totalAmount",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (address.isBlank()) {
                                        addressError = true
                                    } else {
                                        isPlacingOrder = true
                                        cartVm.placeOrder(
                                            address = address,
                                            onSuccess = {
                                                isPlacingOrder = false
                                                onOrderPlaced()
                                            },
                                            onError = { error ->
                                                isPlacingOrder = false
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(error)
                                                }
                                            }
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isPlacingOrder,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                if (isPlacingOrder) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                } else {
                                    Text(
                                        "Confirm Order",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
