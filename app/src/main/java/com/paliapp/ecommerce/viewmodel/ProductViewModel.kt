package com.paliapp.ecommerce.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.paliapp.ecommerce.data.model.Product
import com.paliapp.ecommerce.data.repository.ProductRepository

class ProductViewModel : ViewModel() {

    private val repo = ProductRepository()

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
        repo.getActiveProducts {
            _products.value = it
        }
    }

    fun loadAllProductsForAdmin() {
        repo.getAllProducts {
            _products.value = it
        }
    }

    fun addProduct(product: Product, onDone: (Boolean) -> Unit) {
        repo.addProduct(product) { success ->
            if (success) loadAllProductsForAdmin()
            onDone(success)
        }
    }

    fun updateProduct(product: Product, onDone: (Boolean) -> Unit) {
        repo.updateProduct(product) { success ->
            if (success) loadAllProductsForAdmin()
            onDone(success)
        }
    }

    fun deleteProduct(productId: String, onDone: (Boolean) -> Unit) {
        repo.deleteProduct(productId) { success ->
            if (success) loadAllProductsForAdmin()
            onDone(success)
        }
    }
}
