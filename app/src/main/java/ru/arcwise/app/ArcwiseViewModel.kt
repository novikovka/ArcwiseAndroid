package ru.arcwise.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.room.withTransaction
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.arcwise.app.data.*
import ru.arcwise.app.domain.*

data class AppState(val loading: Boolean = true, val accounts: List<Account> = emptyList(),
    val trades: List<Trade> = emptyList(), val preferences: Preferences = Preferences(), val error: String? = null)
class ArcwiseViewModel(application: Application) : AndroidViewModel(application) {
    private val db = Room.databaseBuilder(application, ArcwiseDatabase::class.java, "arcwise.db").build()
    private val dao = db.ledger()
    private val settings = Settings(application)
    private val failure = MutableStateFlow<String?>(null)
    val state = combine(dao.accounts(), dao.trades(), settings.preferences, failure) { accounts, trades, prefs, error ->
        AppState(accounts.isEmpty(), accounts, trades.map { it.domain() }, prefs, error)
    }.catch { emit(AppState(loading = false, error = "Не удалось прочитать локальные данные. Перезапустите приложение.")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppState())
    init { viewModelScope.launch { runCatching { dao.seed(listOf(Account(1, "Счёт 1"), Account(2, "Счёт 2"))) }
        .onFailure { failure.value = "Не удалось открыть хранилище" } } }
    fun login(value: Boolean) = viewModelScope.launch { runCatching { settings.login(value) }.onFailure { failure.value = "Не удалось сохранить сеанс" } }
    fun dark(value: Boolean) = viewModelScope.launch { runCatching { settings.dark(value) }.onFailure { failure.value = "Не удалось сохранить тему" } }
    fun rename(id: Long, name: String, done: (String?) -> Unit) = viewModelScope.launch {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed.length > 40) { done("Название должно содержать от 1 до 40 символов"); return@launch }
        val result = runCatching { dao.rename(id, trimmed) }
        done(if (result.isSuccess) null else "Не удалось сохранить название")
    }
    fun save(account: Long, ticker: String, type: TradeType, quantity: String, price: String, done: (String?) -> Unit) = viewModelScope.launch {
        val result = runCatching {
            db.withTransaction {
                val trade = Portfolio.validate(account, ticker, type, quantity, price, dao.readTrades().map { it.domain() })
                dao.insert(TradeRow(accountId = trade.accountId, ticker = trade.ticker, type = trade.type.name,
                    quantity = trade.quantity, priceKopecks = trade.priceKopecks, createdAt = trade.createdAt))
            }
        }
        done(result.exceptionOrNull()?.let { if (it is IllegalArgumentException) it.message else "Не удалось сохранить операцию. Попробуйте ещё раз." })
    }
    fun clearError() { failure.value = null }
    override fun onCleared() { db.close(); super.onCleared() }
}
