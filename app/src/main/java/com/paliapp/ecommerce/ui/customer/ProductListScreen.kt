package com.paliapp.ecommerce.ui.customer

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.paliapp.ecommerce.R
import com.paliapp.ecommerce.data.model.Product
import com.paliapp.ecommerce.viewmodel.CartViewModel
import com.paliapp.ecommerce.viewmodel.CategoryViewModel
import com.paliapp.ecommerce.viewmodel.ProductViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
            StickyHeader(
                searchQuery = productVm.searchQuery,
                onSearchChange = { productVm.searchQuery = it },
                cartCount = cartItemsCount,
                onCartClick = onViewCart,
                onLogout = onLogout,
                onSupport = onSupport
            )
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFFBFBFB)),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Trust Badges Section
            item(span = { GridItemSpan(2) }) {
                TrustPropositionBar()
            }

            // Promotional Banner with Swipe
            item(span = { GridItemSpan(2) }) {
                PromoBannerPager()
            }

            // Category Selector
            item(span = { GridItemSpan(2) }) {
                Column {
                    SectionHeader(title = "सामान की कैटेगरी", action = "सब देखें")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        item {
                            CategoryIconChip(
                                name = "सब",
                                icon = Icons.Default.GridView,
                                isSelected = productVm.selectedCategoryId == "All" || productVm.selectedCategoryId.isEmpty(),
                                onClick = { productVm.selectedCategoryId = "All" }
                            )
                        }
                        items(categories) { category ->
                            CategoryIconChip(
                                name = category.name,
                                imageUrl = category.imageUrl,
                                isSelected = productVm.selectedCategoryId == category.id,
                                onClick = { productVm.selectedCategoryId = category.id }
                            )
                        }
                    }
                }
            }

            // Hook Section: Top Wholesale Picks
            if (productVm.searchQuery.isEmpty() && filteredProducts.isNotEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    SectionHeader(title = "सबसे सस्ते थोक दाम", action = "और देखें")
                }
                val topPicks = filteredProducts.take(4)
                items(topPicks) { product ->
                    val quantity = cartItems.find { it.id == product.id }?.qty ?: 0
                    ProductGridItem(
                        product = product,
                        quantity = quantity,
                        onSetQuantity = { finalQty ->
                            val delta = finalQty - quantity
                            if (delta != 0) {
                                cartVm.addToCart(product, delta) { success, message ->
                                    scope.launch {
                                        if (success) {
                                            if (quantity == 0) snackbarHostState.showSnackbar("कार्ट में जोड़ा गया")
                                        } else {
                                            snackbarHostState.showSnackbar(message)
                                        }
                                    }
                                }
                            }
                        },
                        onImageClick = { selectedProductForImages = product }
                    )
                }
            }

            // All Products Section
            item(span = { GridItemSpan(2) }) {
                SectionHeader(
                    title = if (productVm.searchQuery.isNotEmpty()) "सर्च के नतीजे" else "आपके लिए खास",
                    action = ""
                )
            }

            if (filteredProducts.isEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    EmptyState()
                }
            } else {
                items(filteredProducts) { product ->
                    val quantity = cartItems.find { it.id == product.id }?.qty ?: 0
                    ProductGridItem(
                        product = product,
                        quantity = quantity,
                        onSetQuantity = { finalQty ->
                            val delta = finalQty - quantity
                            if (delta != 0) {
                                cartVm.addToCart(product, delta) { success, message ->
                                    scope.launch {
                                        if (success) {
                                            if (quantity == 0) snackbarHostState.showSnackbar("कार्ट में जोड़ा गया")
                                        } else {
                                            snackbarHostState.showSnackbar(message)
                                        }
                                    }
                                }
                            }
                        },
                        onImageClick = { selectedProductForImages = product }
                    )
                }
            }
            
            item(span = { GridItemSpan(2) }) {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickyHeader(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    cartCount: Int,
    onCartClick: () -> Unit,
    onLogout: () -> Unit,
    onSupport: () -> Unit
) {
    Surface(
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = R.mipmap.ic_launcher,
                        contentDescription = "Logo",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "पंडित मार्ट",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onSupport) {
                        Icon(Icons.Default.SupportAgent, contentDescription = "मदद", tint = Color.Gray)
                    }

                    IconButton(
                        onClick = onCartClick,
                        modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), CircleShape)
                    ) {
                        BadgedBox(
                            badge = {
                                if (cartCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.offset(x = (-2).dp, y = 2.dp)
                                    ) {
                                        Text(cartCount.toString(), fontSize = 10.sp)
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.LocalMall, contentDescription = "कार्ट", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "लॉगआउट", tint = Color.Gray)
                    }
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("सबसे सस्ते थोक सामान यहाँ खोजें", fontSize = 14.sp, color = Color.Gray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(54.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                shape = RoundedCornerShape(27.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color(0xFFF5F5F5),
                    focusedContainerColor = Color.White,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                ),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
            )
        }
    }
}

@Composable
fun TrustPropositionBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        TrustItem(Icons.Default.LocalShipping, "फ्री डिलीवरी")
        VerticalDivider(modifier = Modifier.height(14.dp).align(Alignment.CenterVertically), thickness = 1.dp, color = Color.LightGray.copy(alpha = 0.4f))
        TrustItem(Icons.Default.Payments, "सबसे कम दाम")
        VerticalDivider(modifier = Modifier.height(14.dp).align(Alignment.CenterVertically), thickness = 1.dp, color = Color.LightGray.copy(alpha = 0.4f))
        TrustItem(Icons.Default.Verified, "बेस्ट क्वालिटी")
    }
}

@Composable
fun TrustItem(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(11.dp), tint = Color.Gray.copy(alpha = 0.8f))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text, 
            style = MaterialTheme.typography.labelSmall, 
            color = Color.Gray,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp
        )
    }
}

@Composable
fun SectionHeader(title: String, action: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF121212),
            fontSize = 19.sp
        )
        if (action.isNotEmpty()) {
            Text(
                text = action,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { }
            )
        }
    }
}

@Composable
fun CategoryIconChip(
    name: String, 
    icon: ImageVector? = null, 
    imageUrl: String? = null,
    isSelected: Boolean, 
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(76.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(66.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.White)
                .border(
                    BorderStroke(1.dp, if (isSelected) Color.Transparent else Color(0xFFF0F0F0)),
                    RoundedCornerShape(22.dp)
                )
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Icon(
                    icon, 
                    contentDescription = name, 
                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp)
                )
            } else {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    alpha = if (isSelected) 1f else 0.85f
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF424242),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 11.sp
        )
    }
}

@Composable
fun ProductGridItem(
    product: Product, 
    quantity: Int,
    onSetQuantity: (Int) -> Unit,
    onImageClick: () -> Unit
) {
    val isOutOfStock = product.stock <= 0
    var showQuantityDialog by remember { mutableStateOf(false) }

    if (showQuantityDialog) {
        QuantityInputDialog(
            initialQuantity = quantity,
            maxStock = product.stock,
            onDismiss = { showQuantityDialog = false },
            onConfirm = { 
                onSetQuantity(it)
                showQuantityDialog = false
            }
        )
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .shadow(1.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.5f)
                    .clickable { onImageClick() }
            ) {
                AsyncImage(
                    model = product.imageUrls.firstOrNull() ?: product.imageUrl,
                    contentDescription = product.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // Wholesale Badge
                Surface(
                    modifier = Modifier.padding(10.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "थोक दाम",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp
                    )
                }

                if (isOutOfStock) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.75f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            color = Color(0xFF424242).copy(alpha = 0.9f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "खत्म हो गया", 
                                color = Color.White, 
                                fontWeight = FontWeight.Bold, 
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 17.sp,
                        color = Color(0xFF212121)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "₹${product.price}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black,
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "सबसे कम दाम की गारंटी",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            fontSize = 8.sp,
                            modifier = Modifier.padding(bottom = 3.dp).alpha(0.8f)
                        )
                    }
                }

                AnimatedAddButton(
                    quantity = quantity,
                    onAdd = { onSetQuantity(1) },
                    onIncrease = { onSetQuantity(quantity + 1) },
                    onDecrease = { onSetQuantity(quantity - 1) },
                    onManualInput = { showQuantityDialog = true },
                    enabled = !isOutOfStock
                )
            }
        }
    }
}

@Composable
fun AnimatedAddButton(
    quantity: Int,
    onAdd: () -> Unit,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onManualInput: () -> Unit,
    enabled: Boolean
) {
    val haptic = LocalHapticFeedback.current
    
    // Scale animation for "bounce" effect when adding
    val scale by animateFloatAsState(
        targetValue = if (quantity > 0) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "ButtonScale"
    )

    AnimatedContent(
        targetState = quantity > 0,
        transitionSpec = {
            (fadeIn(animationSpec = tween(150)) + scaleIn(initialScale = 0.92f)) togetherWith 
            (fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 0.92f))
        },
        modifier = Modifier.scale(scale),
        label = "AddButtonTransition"
    ) { hasItems ->
        if (hasItems) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDecrease() 
                }, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Text(
                    text = quantity.toString(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { onManualInput() }
                        .padding(horizontal = 8.dp)
                )
                IconButton(onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onIncrease() 
                }, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
        } else {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onAdd()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = enabled,
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = Color(0xFFE0E0E0)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 2.dp)
            ) {
                Text("खोलें", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun QuantityInputDialog(
    initialQuantity: Int,
    maxStock: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var textValue by remember { mutableStateOf(initialQuantity.toString()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("सामान की मात्रा चुनें", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp) },
        text = {
            Column {
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { 
                        if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                            textValue = it
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("0") },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
                val currentQty = textValue.toIntOrNull() ?: 0
                if (currentQty > maxStock) {
                    Text(
                        "सिर्फ $maxStock ही उपलब्ध है",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 6.dp, start = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = textValue.toIntOrNull() ?: 0
                    if (qty <= maxStock) {
                        onConfirm(qty)
                    }
                },
                enabled = textValue.isNotEmpty() && (textValue.toIntOrNull() ?: 0) <= maxStock,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("कार्ट अपडेट करें")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करें", color = Color.Gray)
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = true),
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White
    )
}

@Composable
fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 80.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.SearchOff, contentDescription = null, modifier = Modifier.size(70.dp), tint = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(20.dp))
            Text("कोई सामान नहीं मिला", style = MaterialTheme.typography.bodyLarge, color = Color.Gray, fontWeight = FontWeight.Medium)
            Text("कुछ और सर्च करने की कोशिश करें", style = MaterialTheme.typography.labelMedium, color = Color.LightGray)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PromoBannerPager() {
    val banners = listOf(
        BannerData("40% तक की भारी छूट", "थोक सेल - कुछ समय के लिए!", Color(0xFF1A237E), Color(0xFF0D47A1)),
        BannerData("ज्यादा खरीदें, ज्यादा बचाएं", "थोक आर्डर पर भारी डिस्काउंट", Color(0xFF311B92), Color(0xFF512DA8)),
        BannerData("नया सामान", "आपके लिए नया स्टॉक आ गया है", Color(0xFF004D40), Color(0xFF00796B))
    )
    val pagerState = rememberPagerState(pageCount = { banners.size })
    
    LaunchedEffect(Unit) {
        while(true) {
            delay(5000)
            if (pagerState.pageCount > 0) {
                val nextPage = (pagerState.currentPage + 1) % pagerState.pageCount
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    Column {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(145.dp)
                .clip(RoundedCornerShape(18.dp))
        ) { page ->
            val banner = banners[page]
            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.horizontalGradient(listOf(banner.startColor, banner.endColor)))
                        .padding(22.dp)
                ) {
                    Column(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.75f), verticalArrangement = Arrangement.Center) {
                        Surface(
                            color = Color(0xFFFFD600),
                            shape = RoundedCornerShape(5.dp)
                        ) {
                            Text(
                                "खास मौका",
                                color = Color.Black, 
                                fontWeight = FontWeight.Black, 
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                letterSpacing = 0.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(banner.title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 28.sp, lineHeight = 32.sp)
                        Text(banner.subtitle, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        modifier = Modifier.size(110.dp).align(Alignment.CenterEnd).alpha(0.12f),
                        tint = Color.White
                    )
                }
            }
        }
        
        Row(
            Modifier
                .height(24.dp)
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(banners.size) { iteration ->
                val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else Color(0xFFE0E0E0)
                val width by animateDpAsState(targetValue = if (pagerState.currentPage == iteration) 18.dp else 6.dp, label = "DotWidth")
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .clip(CircleShape)
                        .background(color)
                        .width(width)
                        .height(6.dp)
                )
            }
        }
    }
}

data class BannerData(val title: String, val subtitle: String, val startColor: Color, val endColor: Color)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductImageGalleryDialog(product: Product, onDismiss: () -> Unit) {
    val images = product.imageUrls.ifEmpty { listOf(product.imageUrl) }
    val pagerState = rememberPagerState(pageCount = { images.size })

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                AsyncImage(model = images[page], contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp).statusBarsPadding().align(Alignment.TopEnd),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(product.name, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                IconButton(onClick = onDismiss, modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                }
            }
        }
    }
}
