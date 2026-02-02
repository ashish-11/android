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
import kotlinx.coroutines.launch
import java.util.UUID

class ProductViewModel : ViewModel() {

    private val repo = ProductRepository()
    private val storage = FirebaseStorage.getInstance()

    private val _products = mutableStateOf<List<Product>>(emptyList())
    val products: State<List<Product>> = _products

    var searchQuery by mutableStateOf("")

    val filteredProducts = derivedStateOf {
        if (searchQuery.isEmpty()) {
            _products.value
        } else {
            _products.value.filter { 
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

    fun addProduct(product: Product, imageUri: Uri? = null, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (imageUri != null) {
                uploadImage(imageUri) { url ->
                    if (url != null) {
                        viewModelScope.launch {
                            val result = repo.addProduct(product.copy(imageUrl = url))
                            if (result.isSuccess) loadAllProductsForAdmin()
                            onDone(result.isSuccess)
                        }
                    } else {
                        onDone(false)
                    }
                }
            } else {
                val result = repo.addProduct(product)
                if (result.isSuccess) loadAllProductsForAdmin()
                onDone(result.isSuccess)
            }
        }
    }

    fun updateProduct(product: Product, imageUri: Uri? = null, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (imageUri != null) {
                uploadImage(imageUri) { url ->
                    if (url != null) {
                        viewModelScope.launch {
                            val result = repo.updateProduct(product.copy(imageUrl = url))
                            if (result.isSuccess) loadAllProductsForAdmin()
                            onDone(result.isSuccess)
                        }
                    } else {
                        onDone(false)
                    }
                }
            } else {
                val result = repo.updateProduct(product)
                if (result.isSuccess) loadAllProductsForAdmin()
                onDone(result.isSuccess)
            }
        }
    }

    private fun uploadImage(uri: Uri, onResult: (String?) -> Unit) {
        val fileName = UUID.randomUUID().toString()
        val ref = storage.reference.child("products/$fileName")
        ref.putFile(uri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { url ->
                    onResult(url.toString())
                }.addOnFailureListener { onResult(null) }
            }
            .addOnFailureListener { onResult(null) }
    }

    fun deleteProduct(productId: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repo.deleteProduct(productId)
            if (result.isSuccess) loadAllProductsForAdmin()
            onDone(result.isSuccess)
        }
    }
}
