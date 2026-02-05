package com.paliapp.ecommerce.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paliapp.ecommerce.data.model.CartItem
import com.paliapp.ecommerce.data.model.Product
import com.paliapp.ecommerce.data.repository.CartRepository
import com.paliapp.ecommerce.data.repository.UserRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CartViewModel : ViewModel() {

    private val cartRepo = CartRepository()
    private val userRepo = UserRepository()

    private val _cartItems = mutableStateOf<List<CartItem>>(emptyList())
    val cartItems: State<List<CartItem>> = _cartItems

    private val _userAddress = mutableStateOf("")
    val userAddress: State<String> = _userAddress

    init {
        loadCartItems()
        observeUserAddress()
    }

    private fun observeUserAddress() {
        viewModelScope.launch {
            userRepo.getUserAddress().collectLatest { address ->
                _userAddress.value = address
            }
        }
    }

    fun loadCartItems() {
        viewModelScope.launch {
            _cartItems.value = cartRepo.getCartItems()
        }
    }

    fun addToCart(product: Product, quantity: Int, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = cartRepo.addToCart(product, quantity)
            result.onSuccess {
                loadCartItems()
                onResult(true, "Added to cart")
            }.onFailure {
                onResult(false, it.message ?: "Failed to add to cart")
            }
        }
    }

    fun updateQuantity(productId: String, newQty: Int, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = cartRepo.updateCartItemQuantity(productId, newQty)
            result.onSuccess {
                loadCartItems()
                onResult(true, "Quantity updated")
            }.onFailure {
                onResult(false, it.message ?: "Failed to update quantity")
            }
        }
    }

    fun removeFromCart(itemId: String) {
        viewModelScope.launch {
            cartRepo.removeFromCart(itemId).onSuccess {
                loadCartItems()
            }
        }
    }

    fun placeOrder(address: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val uid = userRepo.getUid()
        val currentItems = _cartItems.value
        
        if (uid == null || currentItems.isEmpty()) {
            onError("Cart is empty or user not logged in")
            return
        }

        viewModelScope.launch {
            val userDetails = userRepo.getUserDetails(uid)
            if (userDetails == null) {
                onError("Failed to fetch user profile")
                return@launch
            }

            val userName = userDetails["name"] as? String ?: ""
            val userEmail = userDetails["email"] as? String ?: ""
            val userMobile = userDetails["mobile"] as? String ?: ""
            
            val orderRepo = com.paliapp.ecommerce.data.repository.OrderRepository()
            val result = orderRepo.placeOrder(
                userId = uid,
                userName = userName,
                userEmail = userEmail,
                userMobile = userMobile,
                items = currentItems,
                address = address
            )

            result.onSuccess {
                loadCartItems()
                onSuccess()
            }.onFailure {
                onError(it.message ?: "Failed to place order")
            }
        }
    }
}
