package com.paliapp.ecommerce.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.FirebaseStorage
import com.paliapp.ecommerce.data.model.Order
import com.paliapp.ecommerce.data.repository.OrderRepository
import com.paliapp.ecommerce.data.repository.SettingsRepository
import com.paliapp.ecommerce.utils.NotificationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class OrderViewModel(application: Application) : AndroidViewModel(application) {

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
    private var isFirstLoad = true

    init {
        observeUpiQr()
    }

    fun loadAllOrders() {
        if (ordersJob?.isActive == true) return
        
        ordersJob = viewModelScope.launch {
            try {
                orderRepo.getAllOrders().collectLatest { newOrders ->
                    if (!isFirstLoad) {
                        // Check for new orders
                        val currentIds = _orders.value.map { it.id }.toSet()
                        newOrders.forEach { order ->
                            if (order.id !in currentIds && order.status == "PLACED") {
                                NotificationHelper.showOrderNotification(
                                    getApplication(),
                                    order.id,
                                    order.userName
                                )
                            }
                        }
                    }
                    _orders.value = newOrders
                    isFirstLoad = false
                }
            } catch (e: Exception) {
                if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    Log.d("OrderViewModel", "Access denied to all orders (likely due to logout or insufficient role).")
                } else {
                    Log.w("OrderViewModel", "Failed to collect all orders.", e)
                }
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
                if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    Log.d("OrderViewModel", "Access denied to UPI QR URL (likely due to logout).")
                } else {
                    Log.w("OrderViewModel", "Failed to collect UPI QR URL.", e)
                }
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
                if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    Log.d("OrderViewModel", "Access denied to customer orders (likely due to logout).")
                } else {
                    Log.w("OrderViewModel", "Failed to collect customer orders.", e)
                }
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

    fun archiveAndDeleteOrders(orders: List<Order>, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = orderRepo.archiveAndDeleteOrders(orders)
            onComplete(result.isSuccess)
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
