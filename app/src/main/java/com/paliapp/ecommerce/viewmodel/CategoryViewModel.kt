package com.paliapp.ecommerce.viewmodel

import android.net.Uri
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.storage.FirebaseStorage
import com.paliapp.ecommerce.data.model.Category
import com.paliapp.ecommerce.data.repository.CategoryRepository
import com.paliapp.ecommerce.data.repository.ProductRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class CategoryViewModel : ViewModel() {
    private val repo = CategoryRepository()
    private val productRepo = ProductRepository()
    private val storage = FirebaseStorage.getInstance()

    private val _categories = mutableStateOf<List<Category>>(emptyList())
    val categories: State<List<Category>> = _categories

    init {
        loadCategories()
    }

    fun loadCategories() {
        viewModelScope.launch {
            _categories.value = repo.getCategories()
        }
    }

    fun addCategory(name: String, imageUri: Uri?, onDone: (Boolean) -> Unit) {
        if (name.isBlank()) {
            onDone(false)
            return
        }
        viewModelScope.launch {
            try {
                var imageUrl = ""
                if (imageUri != null) {
                    imageUrl = uploadImage(imageUri)
                }
                val result = repo.addCategory(Category(name = name, imageUrl = imageUrl))
                if (result.isSuccess) loadCategories()
                onDone(result.isSuccess)
            } catch (e: Exception) {
                onDone(false)
            }
        }
    }

    private suspend fun uploadImage(uri: Uri): String {
        val fileName = UUID.randomUUID().toString()
        val ref = storage.reference.child("categories/$fileName")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    fun updateCategory(category: Category, newImageUri: Uri?, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                var updatedCategory = category
                if (newImageUri != null) {
                    val imageUrl = uploadImage(newImageUri)
                    updatedCategory = category.copy(imageUrl = imageUrl)
                }
                val result = repo.updateCategory(updatedCategory)
                if (result.isSuccess) loadCategories()
                onDone(result.isSuccess)
            } catch (e: Exception) {
                onDone(false)
            }
        }
    }

    fun deleteCategory(categoryId: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repo.deleteCategory(categoryId)
            if (result.isSuccess) {
                // When a category is deleted, reset the categoryId for products using it
                productRepo.resetProductCategory(categoryId)
                loadCategories()
            }
            onDone(result.isSuccess)
        }
    }
}
