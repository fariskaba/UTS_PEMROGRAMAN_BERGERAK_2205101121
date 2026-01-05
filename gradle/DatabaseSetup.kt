package com.example.myapplication

import android.content.Context
import androidx.room.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import java.util.Date

// --- 1. ENTITY (TABEL) ---

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val price: Double,
    val stock: Int
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long, // Room tidak bisa simpan Date langsung, jadi pakai Long (timestamp)
    val itemsJson: String, // Kita simpan list barang sebagai teks JSON
    val totalAmount: Double
)

// --- 2. CONVERTER (PENGUBAH DATA) ---
// Room butuh bantuan untuk simpan List ke String dan sebaliknya
class Converters {
    @TypeConverter
    fun fromCartList(value: List<CartItem>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toCartList(value: String): List<CartItem> {
        val listType = object : TypeToken<List<CartItem>>() {}.type
        return Gson().fromJson(value, listType)
    }
}

// --- 3. DAO (AKSES DATA) ---

@Dao
interface InventoryDao {
    // Produk
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    // Transaksi
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Insert
    suspend fun insertTransaction(transaction: TransactionEntity)
}

// --- 4. DATABASE CONFIG ---

@Database(entities = [ProductEntity::class, TransactionEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun inventoryDao(): InventoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kasir_database"
                )
                    // Hapus baris ini di produksi, ini hanya agar tidak crash jika skema berubah saat dev
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}
