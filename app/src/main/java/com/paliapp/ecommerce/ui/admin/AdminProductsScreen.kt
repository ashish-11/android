package com.paliapp.ecommerce.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paliapp.ecommerce.data.model.Product
import com.paliapp.ecommerce.viewmodel.ProductViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminProductsScreen(
    onBack: () -> Unit,
    vm: ProductViewModel = viewModel()
) {
    val products by vm.products
    var productToEdit by remember { mutableStateOf<Product?>(null) }

    LaunchedEffect(Unit) {
        vm.loadAllProductsForAdmin()
    }

    if (productToEdit != null) {
        EditProductDialog(
            product = productToEdit!!,
            onDismiss = { productToEdit = null },
            onConfirm = { updatedProduct ->
                vm.updateProduct(updatedProduct) {
                    vm.loadAllProductsForAdmin()
                    productToEdit = null
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Products") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(products) { product ->
                ProductAdminItem(
                    product = product,
                    onEdit = { productToEdit = product },
                    onDelete = {
                        vm.deleteProduct(product.id) {
                            vm.loadAllProductsForAdmin()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ProductAdminItem(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = product.name, style = MaterialTheme.typography.titleMedium)
                Text(text = "Price: ₹${product.price}", style = MaterialTheme.typography.bodyMedium)
                Text(text = "Stock: ${product.stock}", style = MaterialTheme.typography.bodySmall)
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun EditProductDialog(
    product: Product,
    onDismiss: () -> Unit,
    onConfirm: (Product) -> Unit
) {
    var name by remember { mutableStateOf(product.name) }
    var price by remember { mutableStateOf(product.price.toString()) }
    var stock by remember { mutableStateOf(product.stock.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Product") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price") })
                OutlinedTextField(value = stock, onValueChange = { stock = it }, label = { Text("Stock") })
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(
                    product.copy(
                        name = name,
                        price = price.toDoubleOrNull() ?: 0.0,
                        stock = stock.toIntOrNull() ?: 0
                    )
                )
            }) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
