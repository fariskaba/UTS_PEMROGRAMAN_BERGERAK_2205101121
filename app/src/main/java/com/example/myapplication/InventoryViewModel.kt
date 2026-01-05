package com.example.myapplication

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.google.gson.Gson

@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).inventoryDao()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val products = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) dao.getAllProducts() else dao.searchProducts(query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions = dao.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChanged(query: String) { _searchQuery.value = query }

    fun addProduct(name: String, price: Double, stock: Int) {
        viewModelScope.launch {
            dao.insertProduct(ProductEntity(name = name, price = price, stock = stock))
        }
    }

    // FITUR BARU: Update Produk (Edit)
    fun updateProduct(product: ProductEntity) {
        viewModelScope.launch {
            dao.updateProduct(product)
        }
    }

    // FITUR BARU: Hapus Produk
    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            dao.deleteProduct(product)
        }
    }

    fun decreaseStock(product: ProductEntity) {
        viewModelScope.launch {
            if (product.stock > 0) dao.updateProduct(product.copy(stock = product.stock - 1))
        }
    }

    fun saveTransaction(items: List<CartItem>, total: Double) {
        viewModelScope.launch {
            val trx = TransactionEntity(
                date = System.currentTimeMillis(),
                itemsJson = Gson().toJson(items),
                totalAmount = total
            )
            dao.insertTransaction(trx)
        }
    }
}
