package com.paliapp.ecommerce.ui.customer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.paliapp.ecommerce.data.model.Product
import com.paliapp.ecommerce.viewmodel.CartViewModel
import com.paliapp.ecommerce.viewmodel.CategoryViewModel
import com.paliapp.ecommerce.viewmodel.ProductViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    onViewCart: () -> Unit,
    onLogout: () -> Unit,
    onSupport: () -> Unit,
    productVm: ProductViewModel = viewModel(),
    cartVm: CartViewModel = viewModel(),
    categoryVm: CategoryViewModel = viewModel()
) {
    val filteredProducts by productVm.filteredProducts
    val cartItems by cartVm.cartItems
    val cartItemsCount = cartItems.sumOf { it.qty }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    var isSearching by remember { mutableStateOf(false) }
    var selectedProductForImages by remember { mutableStateOf<Product?>(null) }
    
    val categories by categoryVm.categories

    LaunchedEffect(Unit) {
        productVm.loadProducts()
        categoryVm.loadCategories()
    }

    if (selectedProductForImages != null) {
        ProductImageGalleryDialog(
            product = selectedProductForImages!!,
            onDismiss = { selectedProductForImages = null }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (isSearching) {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = productVm.searchQuery,
                            onValueChange = { productVm.searchQuery = it },
                            placeholder = { Text("Search products...") },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                IconButton(onClick = { 
                                    productVm.searchQuery = ""
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
                    title = { Text("Wholesale Pandit Mart") },
                    actions = {
                        IconButton(onClick = { isSearching = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = onSupport) {
                            Icon(Icons.Default.SupportAgent, contentDescription = "Support")
                        }
                        IconButton(onClick = onViewCart) {
                            BadgedBox(
                                badge = {
                                    if (cartItemsCount > 0) {
                                        Badge {
                                            Text(text = cartItemsCount.toString())
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = "Cart")
                            }
                        }
                        IconButton(onClick = onLogout) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Category Selector
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = productVm.selectedCategoryId == "All" || productVm.selectedCategoryId.isEmpty(),
                        onClick = { productVm.selectedCategoryId = "All" },
                        label = { Text("All") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
                items(categories) { category ->
                    FilterChip(
                        selected = productVm.selectedCategoryId == category.id,
                        onClick = { productVm.selectedCategoryId = category.id },
                        label = { Text(category.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            if (filteredProducts.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No products found")
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.background),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredProducts) { product ->
                        ProductGridItem(
                            product = product,
                            onAddToCart = { qty ->
                                cartVm.addToCart(product, qty) { success, message ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = message,
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                            },
                            onImageClick = {
                                selectedProductForImages = product
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProductGridItem(
    product: Product, 
    onAddToCart: (Int) -> Unit,
    onImageClick: () -> Unit
) {
    var quantity by remember { mutableIntStateOf(1) }
    var isAdding by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val isOutOfStock = product.stock <= 0
    val displayImage = product.imageUrls.firstOrNull() ?: product.imageUrl

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            // Product Image using Coil AsyncImage
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { onImageClick() },
                contentAlignment = Alignment.Center
            ) {
                if (displayImage.isNotEmpty()) {
                    AsyncImage(
                        model = displayImage,
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    if (product.imageUrls.size > 1) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp),
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "1/${product.imageUrls.size}",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                } else {
                    Icon(
                        Icons.Default.Inventory, 
                        contentDescription = null, 
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                }
                
                if (isOutOfStock) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "OUT OF STOCK",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "₹${product.price}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 4.dp)
                    ) {
                        IconButton(
                            onClick = { if (quantity > 1) quantity-- },
                            modifier = Modifier.size(32.dp),
                            enabled = !isOutOfStock
                        ) {
                            Text(
                                text = "—",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                        Text(
                            text = quantity.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        IconButton(
                            onClick = { if (quantity < product.stock) quantity++ },
                            modifier = Modifier.size(32.dp),
                            enabled = !isOutOfStock
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }

                    Button(
                        onClick = { 
                            if (!isAdding && !isOutOfStock) {
                                isAdding = true
                                onAddToCart(quantity)
                                scope.launch {
                                    delay(1000)
                                    isAdding = false
                                }
                            }
                        },
                        modifier = Modifier.height(40.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAdding) Color(0xFF4CAF50) else MaterialTheme.colorScheme.secondary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isOutOfStock
                    ) {
                        if (isAdding) {
                            Text("Added", style = MaterialTheme.typography.labelMedium)
                        } else {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductImageGalleryDialog(product: Product, onDismiss: () -> Unit) {
    val images = if (product.imageUrls.isNotEmpty()) product.imageUrls else listOf(product.imageUrl)
    val pagerState = rememberPagerState(pageCount = { images.size })

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                AsyncImage(
                    model = images[page],
                    contentDescription = "Product Image $page",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            // Indicator Dots
            if (images.size > 1) {
                Row(
                    Modifier
                        .height(50.dp)
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(images.size) { iteration ->
                        val color = if (pagerState.currentPage == iteration) Color.White else Color.Gray
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(color)
                                .size(8.dp)
                        )
                    }
                }
            }

            // Header with Name and Close Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopStart),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = product.name,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }
    }
}
