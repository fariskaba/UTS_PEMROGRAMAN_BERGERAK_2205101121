package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.MyApplicationTheme

// 1. Model Data Produk
data class Product(
    val id: Long,
    val name: String,
    val price: Double,
    var stock: Int
)

// 2. Model Data Item Keranjang
data class CartItem(
    val product: Product,
    var quantity: Int
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CashierInventorySystem(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun CashierInventorySystem(modifier: Modifier = Modifier) {
    // State untuk Inventory (Database Sementara)
    // [web:5][web:6] Menggunakan state list agar UI terupdate otomatis saat data berubah
    val inventory = remember {
        mutableStateListOf(
            Product(1, "Beras 5kg", 65000.0, 10),
            Product(2, "Minyak Goreng 1L", 18000.0, 24),
            Product(3, "Gula Pasir 1kg", 14500.0, 15),
            Product(4, "Telur 1kg", 28000.0, 30)
        )
    }

    // State untuk Keranjang Belanja
    val cart = remember { mutableStateListOf<CartItem>() }

    // State untuk Form Tambah Produk
    var newName by remember { mutableStateOf("") }
    var newPrice by remember { mutableStateOf("") }
    var newStock by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        // --- Header ---
        Text(
            text = "Sistem Kasir & Inventory",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // --- Bagian 1: Form Tambah Produk (Admin) ---
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Tambah Produk Baru", fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Nama") },
                        modifier = Modifier.weight(2f)
                    )
                    OutlinedTextField(
                        value = newPrice,
                        onValueChange = { newPrice = it },
                        label = { Text("Rp") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newStock,
                        onValueChange = { newStock = it },
                        label = { Text("Stok") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (newName.isNotEmpty() && newPrice.isNotEmpty() && newStock.isNotEmpty()) {
                                inventory.add(
                                    Product(
                                        id = System.currentTimeMillis(),
                                        name = newName,
                                        price = newPrice.toDoubleOrNull() ?: 0.0,
                                        stock = newStock.toIntOrNull() ?: 0
                                    )
                                )
                                // Reset form
                                newName = ""
                                newPrice = ""
                                newStock = ""
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("Simpan")
                    }
                }
            }
        }

        // --- Bagian 2: Daftar Inventory (Katalog) ---
        Text("Katalog Produk (Klik untuk tambah ke kasir)", fontWeight = FontWeight.Bold)
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(inventory) { product ->
                ProductItemRow(product = product, onAddToCart = {
                    if (product.stock > 0) {
                        // Logika Kasir: Kurangi stok, tambah ke cart
                        product.stock--
                        val existingItem = cart.find { it.product.id == product.id }
                        if (existingItem != null) {
                            // Update qty jika sudah ada (trik untuk state list agar refresh)
                            val index = cart.indexOf(existingItem)
                            cart[index] = existingItem.copy(quantity = existingItem.quantity + 1)
                        } else {
                            cart.add(CartItem(product, 1))
                        }
                    }
                })
            }
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        // --- Bagian 3: Keranjang & Total (Kasir) ---
        Text("Keranjang Belanja", fontWeight = FontWeight.Bold)
        LazyColumn(modifier = Modifier.height(150.dp).fillMaxWidth()) {
            items(cart) { item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${item.product.name} x${item.quantity}")
                    Text("Rp ${item.product.price * item.quantity}")
                }
            }
        }

        // Total Bayar
        val total = cart.sumOf { it.product.price * it.quantity }
        Button(
            onClick = { /* Implementasi cetak struk/simpan ke DB */ },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text("Total Bayar: Rp $total", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ProductItemRow(product: Product, onAddToCart: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(product.name, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                Text("Stok: ${product.stock} | Rp ${product.price}", style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onAddToCart, enabled = product.stock > 0) {
                Text("Beli")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun InventoryPreview() {
    MyApplicationTheme {
        CashierInventorySystem()
    }
}
