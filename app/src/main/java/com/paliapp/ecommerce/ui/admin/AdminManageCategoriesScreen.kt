package com.paliapp.ecommerce.ui.admin

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.paliapp.ecommerce.data.model.Category
import com.paliapp.ecommerce.viewmodel.CategoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminManageCategoriesScreen(
    onBack: () -> Unit,
    vm: CategoryViewModel = viewModel()
) {
    val categories by vm.categories
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<Category?>(null) }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }
    var inputName by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(Unit) {
        vm.loadCategories()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("कैटेगरी मैनेजमेंट") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "पीछे")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                inputName = ""
                selectedImageUri = null
                showAddDialog = true 
            }) {
                Icon(Icons.Default.Add, contentDescription = "कैटेगरी जोड़ें")
            }
        }
    ) { padding ->
        if (categories.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("कोई कैटेगरी नहीं मिली। + बटन दबाकर जोड़ें।")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = category.imageUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(Color.LightGray),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = category.name, 
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Row {
                                IconButton(onClick = { 
                                    inputName = category.name
                                    selectedImageUri = null
                                    categoryToEdit = category 
                                }) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "बदलें",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = { categoryToDelete = category }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "हटाएं",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        CategoryDialog(
            title = "नई कैटेगरी जोड़ें",
            name = inputName,
            onNameChange = { inputName = it },
            imageUri = selectedImageUri,
            onImageSelect = { selectedImageUri = it },
            onDismiss = { showAddDialog = false },
            onConfirm = {
                vm.addCategory(inputName, selectedImageUri) { success ->
                    if (success) {
                        showAddDialog = false
                    } else {
                        Toast.makeText(context, "कैटेगरी जोड़ने में विफल", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    if (categoryToEdit != null) {
        CategoryDialog(
            title = "कैटेगरी बदलें",
            name = inputName,
            onNameChange = { inputName = it },
            imageUri = selectedImageUri,
            existingImageUrl = categoryToEdit?.imageUrl,
            onImageSelect = { selectedImageUri = it },
            onDismiss = { categoryToEdit = null },
            onConfirm = {
                vm.updateCategory(categoryToEdit!!.copy(name = inputName), selectedImageUri) { success ->
                    if (success) {
                        categoryToEdit = null
                    } else {
                        Toast.makeText(context, "अपडेट करने में विफल", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    if (categoryToDelete != null) {
        var isDeleting by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!isDeleting) categoryToDelete = null },
            title = { Text("कैटेगरी हटाएं") },
            text = { Text("क्या आप सच में हटाना चाहते हैं? इस कैटेगरी के सामान की लिंक हट जाएगी।") },
            confirmButton = {
                Button(
                    onClick = {
                        isDeleting = true
                        vm.deleteCategory(categoryToDelete!!.id) { success ->
                            isDeleting = false
                            if (success) {
                                categoryToDelete = null
                            } else {
                                Toast.makeText(context, "हटाने में विफल", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !isDeleting,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { 
                    if (isDeleting) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    else Text("हटाएं")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }, enabled = !isDeleting) { Text("रद्द करें") }
            }
        )
    }
}

@Composable
fun CategoryDialog(
    title: String,
    name: String,
    onNameChange: (String) -> Unit,
    imageUri: Uri?,
    existingImageUrl: String? = null,
    onImageSelect: (Uri?) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> onImageSelect(uri) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text(title) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray)
                        .clickable(enabled = !isLoading) { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUri != null) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (!existingImageUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = existingImageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(40.dp))
                    }
                }
                Text("आइकन चुनने के लिए टच करें", style = MaterialTheme.typography.labelSmall)
                
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("कैटेगरी का नाम") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isLoading = true
                    onConfirm()
                },
                enabled = !isLoading && name.isNotBlank()
            ) { 
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                else Text("सुरक्षित करें") 
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) { Text("रद्द करें") }
        }
    )
}
