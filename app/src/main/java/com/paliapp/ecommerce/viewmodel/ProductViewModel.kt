package com.paliapp.ecommerce.viewmodel

import android.net.Uri
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.storage.FirebaseStorage
import com.paliapp.ecommerce.data.model.Product
import com.paliapp.ecommerce.data.repository.ProductRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ProductViewModel : ViewModel() {

    private val repo = ProductRepository()
    private val storage = FirebaseStorage.getInstance()

    private val _products = mutableStateOf<List<Product>>(emptyList())
    val products: State<List<Product>> = _products

    var searchQuery by mutableStateOf("")
    var selectedCategoryId by mutableStateOf("") // Now using ID instead of name

    val filteredProducts = derivedStateOf {
        val filteredByCategory = if (selectedCategoryId.isEmpty() || selectedCategoryId == "All") {
            _products.value
        } else {
            _products.value.filter { it.categoryId == selectedCategoryId }
        }

        if (searchQuery.isEmpty()) {
            filteredByCategory
        } else {
            filteredByCategory.filter { 
                it.name.contains(searchQuery, ignoreCase = true) 
            }
        }
    }

    fun loadProducts() {
        viewModelScope.launch {
            _products.value = repo.getActiveProducts()
        }
    }

    fun loadAllProductsForAdmin() {
        viewModelScope.launch {
            _products.value = repo.getAllProducts()
        }
    }

    fun addProduct(product: Product, imageUris: List<Uri> = emptyList(), onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val urls = uploadMultipleImages(imageUris)
                val newProduct = product.copy(
                    imageUrls = urls,
                    imageUrl = urls.firstOrNull() ?: product.imageUrl
                )
                val result = repo.addProduct(newProduct)
                if (result.isSuccess) loadAllProductsForAdmin()
                onDone(result.isSuccess)
            } catch (e: Exception) {
                onDone(false)
            }
        }
    }

    fun updateProduct(product: Product, newImageUris: List<Uri> = emptyList(), onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val newUrls = uploadMultipleImages(newImageUris)
                // Combine existing URLs that weren't replaced, up to 5 total
                val totalUrls = (product.imageUrls + newUrls).take(5)
                val updatedProduct = product.copy(
                    imageUrls = totalUrls,
                    imageUrl = totalUrls.firstOrNull() ?: product.imageUrl
                )
                val result = repo.updateProduct(updatedProduct)
                if (result.isSuccess) loadAllProductsForAdmin()
                onDone(result.isSuccess)
            } catch (e: Exception) {
                onDone(false)
            }
        }
    }

    private suspend fun uploadMultipleImages(uris: List<Uri>): List<String> {
        return uris.take(5).map { uri ->
            viewModelScope.async {
                val fileName = UUID.randomUUID().toString()
                val ref = storage.reference.child("products/$fileName")
                ref.putFile(uri).await()
                ref.downloadUrl.await().toString()
            }
        }.awaitAll()
    }

    fun deleteProduct(productId: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repo.deleteProduct(productId)
            if (result.isSuccess) loadAllProductsForAdmin()
            onDone(result.isSuccess)
        }
    }
}
