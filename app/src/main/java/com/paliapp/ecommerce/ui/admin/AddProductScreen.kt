package com.paliapp.ecommerce.ui.admin

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.unit.dp
import com.paliapp.ecommerce.data.model.Product
import com.paliapp.ecommerce.viewmodel.ProductViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    onBack: () -> Unit,
    vm: ProductViewModel = viewModel()
) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Product") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Product Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            
            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                label = { Text("Price (₹)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            
            OutlinedTextField(
                value = stock,
                onValueChange = { stock = it },
                label = { Text("Initial Stock") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    vm.addProduct(
                        Product(
                            name = name,
                            price = price.toDoubleOrNull() ?: 0.0,
                            stock = stock.toIntOrNull() ?: 100
                        ),
                        imageUri = imageUri
                    ) { success ->
                        if (success) {
                            onBack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Product")
            }
        }
    }
}
