package ru.arcwise.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import ru.arcwise.app.domain.*

@Entity(tableName = "accounts")
data class Account(@PrimaryKey val id: Long, val name: String)
@Entity(tableName = "trades", foreignKeys = [ForeignKey(entity = Account::class, parentColumns = ["id"], childColumns = ["accountId"])], indices = [Index("accountId")])
data class TradeRow(@PrimaryKey(autoGenerate = true) val id: Long = 0, val accountId: Long,
    val ticker: String, val type: String, val quantity: Long, val priceKopecks: Long, val createdAt: Long) {
    fun domain() = Trade(id, accountId, ticker, TradeType.valueOf(type), quantity, priceKopecks, createdAt)
}
@Dao
interface LedgerDao {
    @Query("SELECT * FROM accounts ORDER BY id") fun accounts(): Flow<List<Account>>
    @Query("SELECT * FROM trades ORDER BY createdAt, id") fun trades(): Flow<List<TradeRow>>
    @Query("SELECT * FROM trades ORDER BY createdAt, id") suspend fun readTrades(): List<TradeRow>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun seed(accounts: List<Account>)
    @Query("UPDATE accounts SET name = :name WHERE id = :id") suspend fun rename(id: Long, name: String)
    @Insert suspend fun insert(row: TradeRow)
}
@Database(entities = [Account::class, TradeRow::class], version = 1, exportSchema = true)
abstract class ArcwiseDatabase : RoomDatabase() {
    abstract fun ledger(): LedgerDao
}
private val Context.settings by preferencesDataStore("arcwise_settings")
data class Preferences(val dark: Boolean = false, val loggedIn: Boolean = false)
class Settings(context: Context) {
    private val store = context.settings
    private val darkKey = booleanPreferencesKey("dark_theme")
    private val loginKey = booleanPreferencesKey("demo_login")
    val preferences = store.data.catch { if (it is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw it }
        .map { Preferences(it[darkKey] ?: false, it[loginKey] ?: false) }
    suspend fun dark(value: Boolean) { store.edit { it[darkKey] = value } }
    suspend fun login(value: Boolean) { store.edit { it[loginKey] = value } }
}
