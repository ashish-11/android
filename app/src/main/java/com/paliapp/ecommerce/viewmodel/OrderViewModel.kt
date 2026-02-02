package com.paliapp.ecommerce.viewmodel

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.storage.FirebaseStorage
import com.paliapp.ecommerce.data.model.Order
import com.paliapp.ecommerce.data.repository.OrderRepository
import com.paliapp.ecommerce.data.repository.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class OrderViewModel : ViewModel() {

    private val orderRepo = OrderRepository()
    private val settingsRepo = SettingsRepository()
    private val storage = FirebaseStorage.getInstance()

    private val _orders = mutableStateOf<List<Order>>(emptyList())
    val orders: State<List<Order>> = _orders

    private val _customerOrders = mutableStateOf<List<Order>>(emptyList())
    val customerOrders: State<List<Order>> = _customerOrders

    val upiQrUrl = mutableStateOf<String?>(null)
    
    private var ordersJob: Job? = null
    private var customerOrdersJob: Job? = null

    init {
        loadAllOrders()
        observeUpiQr()
    }

    fun loadAllOrders() {
        ordersJob?.cancel()
        ordersJob = viewModelScope.launch {
            try {
                orderRepo.getAllOrders().collectLatest {
                    _orders.value = it
                }
            } catch (e: Exception) {
                Log.w("OrderViewModel", "Failed to collect all orders, likely due to logout.", e)
                _orders.value = emptyList()
            }
        }
    }

    private fun observeUpiQr() {
        viewModelScope.launch {
             try {
                settingsRepo.getUpiQrUrlFlow().collectLatest {
                    upiQrUrl.value = it
                }
            } catch (e: Exception) {
                Log.w("OrderViewModel", "Failed to collect UPI QR URL.", e)
                upiQrUrl.value = null
            }
        }
    }

    fun loadCustomerOrders(userId: String) {
        customerOrdersJob?.cancel()
        customerOrdersJob = viewModelScope.launch {
            try {
                orderRepo.getCustomerOrders(userId).collectLatest {
                    _customerOrders.value = it
                }
            } catch (e: Exception) {
                Log.w("OrderViewModel", "Failed to collect customer orders, likely due to logout.", e)
                _customerOrders.value = emptyList()
            }
        }
    }
    
    fun updateOrderStatus(orderId: String, newStatus: String) {
        viewModelScope.launch {
            orderRepo.updateOrderStatus(orderId, newStatus)
        }
    }

    fun updatePaymentStatus(orderId: String, newStatus: String) {
        viewModelScope.launch {
            orderRepo.updatePaymentStatus(orderId, newStatus)
        }
    }

    fun updateDeliveryDate(orderId: String, date: String) {
        viewModelScope.launch {
            orderRepo.updateDeliveryDate(orderId, date)
        }
    }

    fun setPaymentMethod(orderId: String, method: String) {
        viewModelScope.launch {
            orderRepo.setPaymentMethod(orderId, method)
        }
    }

    fun deleteOrder(orderId: String) {
        viewModelScope.launch {
            orderRepo.deleteOrder(orderId)
        }
    }

    fun uploadQrCode(uri: Uri, onResult: (Boolean) -> Unit) {
        val ref = storage.reference.child("settings/upi_qr.png")
        ref.putFile(uri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { url ->
                    updateUpiQr(url.toString())
                    onResult(true)
                }.addOnFailureListener { onResult(false) }
            }
            .addOnFailureListener { onResult(false) }
    }

    private fun updateUpiQr(url: String) {
        viewModelScope.launch {
            settingsRepo.updateUpiQrUrl(url)
        }
    }

    override fun onCleared() {
        super.onCleared()
        ordersJob?.cancel()
        customerOrdersJob?.cancel()
    }
}
