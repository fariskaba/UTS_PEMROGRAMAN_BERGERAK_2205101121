package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*

data class CartItem(val product: ProductEntity, var quantity: Int)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val viewModel: InventoryViewModel = viewModel()
                    CashierAppMain(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun CashierAppMain(viewModel: InventoryViewModel, modifier: Modifier = Modifier) {
    val transactionHistory by viewModel.transactions.collectAsState()
    val cart = remember { mutableStateListOf<CartItem>() }
    var currentScreen by remember { mutableStateOf("cashier") }
    var showReceiptDialog by remember { mutableStateOf<TransactionEntity?>(null) }

    // Dialog State untuk Edit Produk
    var productToEdit by remember { mutableStateOf<ProductEntity?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        NavigationBar {
            NavigationBarItem(
                icon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) },
                label = { Text("Kasir") },
                selected = currentScreen == "cashier",
                onClick = { currentScreen = "cashier" }
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.List, contentDescription = null) },
                label = { Text("Riwayat") },
                selected = currentScreen == "history",
                onClick = { currentScreen = "history" }
            )
        }

        if (currentScreen == "cashier") {
            CashierScreen(
                viewModel = viewModel,
                cart = cart,
                onAddProduct = { name, price, stock -> viewModel.addProduct(name, price, stock) },
                onEditProductClick = { product -> productToEdit = product }, // Trigger dialog
                onAddToCart = { product ->
                    viewModel.decreaseStock(product)
                    val existing = cart.find { it.product.id == product.id }
                    if (existing != null) {
                        val idx = cart.indexOf(existing)
                        cart[idx] = existing.copy(quantity = existing.quantity + 1)
                    } else {
                        cart.add(CartItem(product, 1))
                    }
                },
                onUpdateCartQty = { item, change ->
                    val idx = cart.indexOf(item)
                    if (idx != -1) {
                        val newQty = item.quantity + change
                        if (newQty > 0) {
                            if (change > 0) {
                                if (item.product.stock > 0) {
                                    viewModel.decreaseStock(item.product)
                                    cart[idx] = item.copy(quantity = newQty)
                                }
                            } else {
                                cart[idx] = item.copy(quantity = newQty)
                            }
                        }
                    }
                },
                onRemoveFromCart = { item -> cart.remove(item) },
                onCheckout = {
                    if (cart.isNotEmpty()) {
                        val total = cart.sumOf { it.product.price * it.quantity }
                        viewModel.saveTransaction(cart.toList(), total)
                        val tempTrx = TransactionEntity(
                            date = System.currentTimeMillis(),
                            itemsJson = Gson().toJson(cart.toList()),
                            totalAmount = total
                        )
                        showReceiptDialog = tempTrx
                        cart.clear()
                    }
                }
            )
        } else {
            HistoryScreen(
                transactions = transactionHistory,
                onTransactionClick = { showReceiptDialog = it }
            )
        }
    }

    // --- DIALOGS ---
    if (showReceiptDialog != null) {
        ReceiptDialog(transaction = showReceiptDialog!!, onDismiss = { showReceiptDialog = null })
    }

    // Dialog Edit Produk
    if (productToEdit != null) {
        EditProductDialog(
            product = productToEdit!!,
            onDismiss = { productToEdit = null },
            onSave = { updatedProduct ->
                viewModel.updateProduct(updatedProduct)
                productToEdit = null
            },
            onDelete = {
                viewModel.deleteProduct(it)
                productToEdit = null
            }
        )
    }
}

@Composable
fun CashierScreen(
    viewModel: InventoryViewModel,
    cart: List<CartItem>,
    onAddProduct: (String, Double, Int) -> Unit,
    onEditProductClick: (ProductEntity) -> Unit,
    onAddToCart: (ProductEntity) -> Unit,
    onUpdateCartQty: (CartItem, Int) -> Unit,
    onRemoveFromCart: (CartItem) -> Unit,
    onCheckout: () -> Unit
) {
    val inventory by viewModel.products.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var newName by remember { mutableStateOf("") }
    var newPrice by remember { mutableStateOf("") }
    var newStock by remember { mutableStateOf("") }
    val total = cart.sumOf { it.product.price * it.quantity }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Form Input
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text("Tambah Produk Baru", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Nama") }, modifier = Modifier.weight(2f), singleLine = true)
                    OutlinedTextField(value = newPrice, onValueChange = { newPrice = it }, label = { Text("Rp") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1.2f), singleLine = true)
                    OutlinedTextField(value = newStock, onValueChange = { newStock = it }, label = { Text("Qty") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                }
                Button(onClick = {
                    if (newName.isNotEmpty()) {
                        onAddProduct(newName, newPrice.toDoubleOrNull() ?: 0.0, newStock.toIntOrNull() ?: 0)
                        newName = ""; newPrice = ""; newStock = ""
                    }
                }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(40.dp)) { Text("Simpan") }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            placeholder = { Text("Cari produk...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) { Icon(Icons.Default.Close, contentDescription = "Clear") }
                }
            },
            shape = RoundedCornerShape(8.dp),
            singleLine = true
        )

        // List Produk
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(inventory) { product ->
                Card(modifier = Modifier.padding(vertical = 2.dp).fillMaxWidth()) {
                    Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(product.name, fontWeight = FontWeight.Medium)
                            Text("Sisa: ${product.stock} | Rp ${product.price.toInt()}", style = MaterialTheme.typography.bodySmall)
                        }

                        Row {
                            // Tombol Edit (Pensil)
                            IconButton(onClick = { onEditProductClick(product) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray)
                            }
                            // Tombol Beli
                            Button(onClick = { onAddToCart(product) }, enabled = product.stock > 0, contentPadding = PaddingValues(horizontal = 12.dp), modifier = Modifier.height(35.dp)) {
                                Text("Pilih")
                            }
                        }
                    }
                }
            }
        }

        Divider(thickness = 2.dp, color = Color.LightGray)

        // Keranjang Belanja
        Text("Keranjang Belanja", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        LazyColumn(modifier = Modifier.height(180.dp).fillMaxWidth()) {
            items(cart) { item ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)), modifier = Modifier.padding(vertical = 2.dp).fillMaxWidth()) {
                    Row(modifier = Modifier.padding(8.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.product.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Rp ${item.product.price * item.quantity}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onUpdateCartQty(item, -1) }, modifier = Modifier.size(30.dp)) { Icon(Icons.Default.KeyboardArrowDown, "Kurang") }
                            Text(text = "${item.quantity}", modifier = Modifier.padding(horizontal = 8.dp), fontWeight = FontWeight.Bold)
                            IconButton(onClick = { onUpdateCartQty(item, 1) }, modifier = Modifier.size(30.dp)) { Icon(Icons.Default.KeyboardArrowUp, "Tambah") }
                        }
                        IconButton(onClick = { onRemoveFromCart(item) }, modifier = Modifier.size(30.dp)) { Icon(Icons.Default.Delete, "Hapus", tint = Color.Red) }
                    }
                }
            }
        }

        Button(onClick = onCheckout, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary), enabled = cart.isNotEmpty()) {
            Text("BAYAR (Total: Rp ${total.toInt()})", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// Dialog Edit Produk (UI Popup)
@Composable
fun EditProductDialog(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onSave: (ProductEntity) -> Unit,
    onDelete: (ProductEntity) -> Unit
) {
    var name by remember { mutableStateOf(product.name) }
    var price by remember { mutableStateOf(product.price.toString()) }
    var stock by remember { mutableStateOf(product.stock.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Edit Produk", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 16.dp))

                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Produk") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Harga") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = stock, onValueChange = { stock = it }, label = { Text("Stok") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Button(onClick = { onDelete(product) }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                        Text("Hapus")
                    }
                    Button(onClick = {
                        val newPrice = price.toDoubleOrNull() ?: product.price
                        val newStock = stock.toIntOrNull() ?: product.stock
                        onSave(product.copy(name = name, price = newPrice, stock = newStock))
                    }) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}

// History & Receipt Screens (Sama seperti sebelumnya)
@Composable
fun HistoryScreen(transactions: List<TransactionEntity>, onTransactionClick: (TransactionEntity) -> Unit) {
    LazyColumn(modifier = Modifier.padding(16.dp)) {
        items(transactions) { trx ->
            Card(modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth().clickable { onTransactionClick(trx) }) {
                Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("ID: ${trx.id}", style = MaterialTheme.typography.bodySmall)
                        Text(SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(trx.date)))
                    }
                    Text("Rp ${trx.totalAmount}", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ReceiptDialog(transaction: TransactionEntity, onDismiss: () -> Unit) {
    val items: List<CartItem> = Gson().fromJson(transaction.itemsJson, object : com.google.gson.reflect.TypeToken<List<CartItem>>() {}.type)
    Dialog(onDismissRequest = onDismiss) {
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("STRUK BELANJA", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Divider(modifier = Modifier.padding(vertical = 16.dp), color = Color.Black)
                Text("Tgl: ${SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(Date(transaction.date))}")
                Spacer(modifier = Modifier.height(16.dp))
                items.forEach { item ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${item.product.name} x${item.quantity}")
                        Text("Rp ${item.product.price * item.quantity}")
                    }
                }
                Divider(modifier = Modifier.padding(vertical = 16.dp), color = Color.Black)
                Text("TOTAL: Rp ${transaction.totalAmount}", fontWeight = FontWeight.Bold)
                Button(onClick = onDismiss, modifier = Modifier.padding(top = 16.dp)) { Text("Tutup") }
            }
        }
    }
}
