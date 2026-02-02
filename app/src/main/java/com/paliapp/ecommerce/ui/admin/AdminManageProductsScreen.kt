package com.paliapp.ecommerce.ui.admin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.paliapp.ecommerce.data.model.Product
import com.paliapp.ecommerce.viewmodel.ProductViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminManageProductsScreen(
    onBack: () -> Unit,
    vm: ProductViewModel = viewModel()
) {
    val filteredProducts by vm.filteredProducts
    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Product?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.loadAllProductsForAdmin()
    }

    Scaffold(
        topBar = {
            if (isSearching) {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = vm.searchQuery,
                            onValueChange = { vm.searchQuery = it },
                            placeholder = { Text("Search products...") },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                IconButton(onClick = { 
                                    vm.searchQuery = ""
                                    isSearching = false 
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close")
                                }
                            }
                        )
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("Manage Products") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearching = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = { vm.loadAllProductsForAdmin() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Product")
            }
        }
    ) { padding ->
        if (filteredProducts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding), 
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (vm.searchQuery.isEmpty()) "No products found" else "No matching products")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { 
                        vm.searchQuery = ""
                        vm.loadAllProductsForAdmin() 
                    }) {
                        Text("Refresh")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredProducts) { product ->
                    ProductManageCard(
                        product = product,
                        onEdit = { productToEdit = product },
                        onDelete = { showDeleteDialog = product }
                    )
                }
                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }

    if (showAddDialog) {
        EditProductDialog(
            title = "Add New Product",
            product = Product(),
            onDismiss = { showAddDialog = false },
            onSave = { newProduct, uri ->
                vm.addProduct(newProduct, uri) {
                    showAddDialog = false
                }
            }
        )
    }

    if (productToEdit != null) {
        EditProductDialog(
            title = "Edit Product",
            product = productToEdit!!,
            onDismiss = { productToEdit = null },
            onSave = { updatedProduct, uri ->
                vm.updateProduct(updatedProduct, uri) {
                    productToEdit = null
                }
            }
        )
    }

    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Product") },
            text = { Text("Are you sure you want to delete '${showDeleteDialog?.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteProduct(showDeleteDialog!!.id) {
                            showDeleteDialog = null
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ProductManageCard(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = null,
                modifier = Modifier.size(60.dp).background(Color.LightGray, RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = "Price: ₹${product.price} / ${product.unit}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "Stock: ${product.stock}", 
                    style = MaterialTheme.typography.bodySmall,
                    color = if (product.stock < 10) MaterialTheme.colorScheme.error else Color.Unspecified
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun EditProductDialog(
    title: String,
    product: Product,
    onDismiss: () -> Unit,
    onSave: (Product, Uri?) -> Unit
) {
    var name by remember { mutableStateOf(product.name) }
    var price by remember { mutableStateOf(if (product.price == 0.0) "" else product.price.toString()) }
    var unit by remember { mutableStateOf(product.unit) }
    var stock by remember { mutableStateOf(if (product.stock == 0) "" else product.stock.toString()) }
    var active by remember { mutableStateOf(product.active) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .align(Alignment.CenterHorizontally)
                        .background(Color.LightGray, RoundedCornerShape(8.dp))
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUri != null) {
                        AsyncImage(model = imageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else if (product.imageUrl.isNotEmpty()) {
                        AsyncImage(model = product.imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(40.dp))
                    }
                }
                Text("Tap to change image", style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.CenterHorizontally))
                
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Unit (kg/bag/etc)") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = stock, onValueChange = { stock = it }, label = { Text("Stock") }, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = active, onCheckedChange = { active = it })
                    Text("Active (Visible to Customers)")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isLoading = true
                    onSave(product.copy(
                        name = name,
                        price = price.toDoubleOrNull() ?: 0.0,
                        unit = unit.ifEmpty { "kg" },
                        stock = stock.toIntOrNull() ?: 0,
                        active = active
                    ), imageUri)
                },
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp)) else Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
