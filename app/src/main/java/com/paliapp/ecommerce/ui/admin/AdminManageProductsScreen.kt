package com.paliapp.ecommerce.ui.admin

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.paliapp.ecommerce.data.model.Product
import com.paliapp.ecommerce.viewmodel.CategoryViewModel
import com.paliapp.ecommerce.viewmodel.ProductViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminManageProductsScreen(
    onBack: () -> Unit,
    onManageCategories: () -> Unit,
    vm: ProductViewModel = viewModel(),
    categoryVm: CategoryViewModel = viewModel()
) {
    val filteredProducts by vm.filteredProducts
    val categories by categoryVm.categories
    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Product?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        vm.loadAllProductsForAdmin()
        categoryVm.loadCategories()
    }

    Scaffold(
        topBar = {
            if (isSearching) {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = vm.searchQuery,
                            onValueChange = { vm.searchQuery = it },
                            placeholder = { Text("सामान खोजें...") },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                IconButton(onClick = { 
                                    vm.searchQuery = ""
                                    isSearching = false 
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "बंद करें")
                                }
                            }
                        )
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("सामान का मैनेजमेंट") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "पीछे")
                        }
                    },
                    actions = {
                        IconButton(onClick = onManageCategories) {
                            Icon(Icons.Default.Category, contentDescription = "कैटेगरी बदलें")
                        }
                        IconButton(onClick = { isSearching = true }) {
                            Icon(Icons.Default.Search, contentDescription = "खोजें")
                        }
                        IconButton(onClick = { 
                            vm.loadAllProductsForAdmin()
                            categoryVm.loadCategories()
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "अपडेट")
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "नया सामान जोड़ें")
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
                    Text(if (vm.searchQuery.isEmpty()) "कोई सामान नहीं मिला" else "मैचिंग सामान नहीं मिला")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { 
                        vm.searchQuery = ""
                        vm.loadAllProductsForAdmin() 
                        categoryVm.loadCategories()
                    }) {
                        Text("अपडेट करें")
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
                        categoryName = categories.find { it.id == product.categoryId }?.name ?: "General",
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
            title = "नया सामान जोड़ें",
            product = Product(),
            onDismiss = { showAddDialog = false },
            onSave = { newProduct, uris ->
                vm.addProduct(newProduct, uris) { success ->
                    if (success) {
                        showAddDialog = false
                    } else {
                        Toast.makeText(context, "सामान जोड़ने में विफल।", Toast.LENGTH_LONG).show()
                    }
                }
            },
            categoryVm = categoryVm
        )
    }

    if (productToEdit != null) {
        EditProductDialog(
            title = "सामान अपडेट करें",
            product = productToEdit!!,
            onDismiss = { productToEdit = null },
            onSave = { updatedProduct, uris ->
                vm.updateProduct(updatedProduct, uris) { success ->
                    if (success) {
                        productToEdit = null
                    } else {
                        Toast.makeText(context, "अपडेट करने में विफल।", Toast.LENGTH_LONG).show()
                    }
                }
            },
            categoryVm = categoryVm
        )
    }

    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("सामान हटाएं") },
            text = { Text("क्या आप सच में '${showDeleteDialog?.name}' को हटाना चाहते हैं?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteProduct(showDeleteDialog!!.id) { success ->
                            if (success) {
                                showDeleteDialog = null
                            } else {
                                Toast.makeText(context, "हटाने में विफल।", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("हटाएं")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("रद्द करें")
                }
            }
        )
    }
}

@Composable
fun ProductManageCard(
    product: Product,
    categoryName: String,
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
                model = product.imageUrl.ifEmpty { if (product.imageUrls.isNotEmpty()) product.imageUrls[0] else "" },
                contentDescription = null,
                modifier = Modifier.size(60.dp).background(Color.LightGray, RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = "कैटेगरी: $categoryName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                Text(text = "दाम: ₹${product.price} / ${product.unit}", style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "स्टॉक: ${product.stock}", 
                        style = MaterialTheme.typography.bodySmall,
                        color = if (product.stock < 10) MaterialTheme.colorScheme.error else Color.Unspecified
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (product.isReturnable) {
                        Surface(
                            color = Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "वापसी: ${product.returnWindowDays} दिन",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "वापसी संभव नहीं",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "बदलें", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "हटाएं", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProductDialog(
    title: String,
    product: Product,
    onDismiss: () -> Unit,
    onSave: (Product, List<Uri>) -> Unit,
    categoryVm: CategoryViewModel = viewModel()
) {
    var name by remember { mutableStateOf(product.name) }
    var selectedCategoryId by remember { mutableStateOf(product.categoryId) }
    var price by remember { mutableStateOf(if (product.price == 0.0) "" else product.price.toString()) }
    var unit by remember { mutableStateOf(product.unit) }
    var stock by remember { mutableStateOf(if (product.stock == 0) "" else product.stock.toString()) }
    var active by remember { mutableStateOf(product.active) }
    var isReturnable by remember { mutableStateOf(product.isReturnable) }
    var returnWindowDays by remember { mutableStateOf(product.returnWindowDays.toString()) }
    
    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    
    val categories by categoryVm.categories
    var expanded by remember { mutableStateOf(false) }
    val selectedCategoryName = categories.find { it.id == selectedCategoryId }?.name ?: "General"

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        imageUris = uris.take(5)
    }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text(title) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                item {
                    Text("सामान की फोटो (अधिकतम 5)", style = MaterialTheme.typography.titleSmall)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        item {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(Color.LightGray, RoundedCornerShape(8.dp))
                                    .clickable(enabled = !isLoading) { launcher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                            }
                        }
                        itemsIndexed(imageUris) { index, uri ->
                            Box(modifier = Modifier.size(80.dp)) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                IconButton(
                                    onClick = { imageUris = imageUris.filterIndexed { i, _ -> i != index } },
                                    modifier = Modifier.size(24.dp).align(Alignment.TopEnd).background(Color.White, CircleShape),
                                    enabled = !isLoading
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "हटाएं", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        if (imageUris.isEmpty() && product.imageUrls.isNotEmpty()) {
                            items(product.imageUrls) { url ->
                                AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
                
                item { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("नाम") }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading) }
                
                item {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { if (!isLoading) expanded = !expanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedCategoryName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("कैटेगरी") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isLoading
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            categories.forEach { selectionOption ->
                                DropdownMenuItem(
                                    text = { Text(selectionOption.name) },
                                    onClick = {
                                        selectedCategoryId = selectionOption.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("दाम") }, modifier = Modifier.weight(1f), enabled = !isLoading, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                        OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("इकाई (जैसे kg)") }, modifier = Modifier.weight(1f), enabled = !isLoading)
                    }
                }
                
                item { OutlinedTextField(value = stock, onValueChange = { stock = it }, label = { Text("स्टॉक") }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) }
                
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = isReturnable, onCheckedChange = { isReturnable = it }, enabled = !isLoading)
                                Text("वापसी संभव है", style = MaterialTheme.typography.bodyMedium)
                            }
                            if (isReturnable) {
                                OutlinedTextField(
                                    value = returnWindowDays,
                                    onValueChange = { returnWindowDays = it },
                                    label = { Text("कितने दिन में वापसी?") },
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                    enabled = !isLoading,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = active, onCheckedChange = { active = it }, enabled = !isLoading)
                        Text("सक्रिय (ग्राहकों को दिखेगा)")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isLoading = true
                    onSave(product.copy(
                        name = name,
                        categoryId = selectedCategoryId,
                        price = price.toDoubleOrNull() ?: 0.0,
                        unit = unit.ifEmpty { "kg" },
                        stock = stock.toIntOrNull() ?: 0,
                        active = active,
                        isReturnable = isReturnable,
                        returnWindowDays = if (isReturnable) (returnWindowDays.toIntOrNull() ?: 0) else 0
                    ), imageUris)
                },
                enabled = !isLoading && name.isNotBlank()
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White) else Text("सुरक्षित करें")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("रद्द करें")
            }
        }
    )
}
