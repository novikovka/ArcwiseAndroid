package ru.arcwise.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.arcwise.app.AppState
import ru.arcwise.app.domain.*
import java.util.Locale

@Composable fun OperationScreen(state: AppState, initialTicker: String, initialAccount: Long?,
    save: (Long, String, TradeType, String, String, (String?) -> Unit) -> Any, done: () -> Unit) {
    var account by rememberSaveable(initialTicker, initialAccount) { mutableLongStateOf(initialAccount ?: state.accounts.first().id) }
    var type by rememberSaveable { mutableStateOf(TradeType.BUY) }
    var ticker by rememberSaveable(initialTicker) { mutableStateOf(initialTicker) }
    var quantity by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val c = LocalArcColors.current
    val available = remember(state.trades, account, ticker) {
        Portfolio.positions(state.trades, account).firstOrNull { it.ticker == ticker.trim().uppercase(Locale.ROOT) }?.quantity ?: 0L
    }
    val total = remember(quantity, price) {
        val q = quantity.toLongOrNull()
        val p = price.trim().replace(',', '.').toBigDecimalOrNull()
        if (q != null && q > 0 && p != null && p.signum() > 0) p.multiply(q.toBigDecimal()) else null
    }
    ScreenColumn {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = done, enabled = !saving) { Icon(Icons.Outlined.Close, "Закрыть операцию") }
            Text("Новая операция", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = c.text)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilterChip(selected = type == TradeType.BUY, onClick = { type = TradeType.BUY; error = null }, enabled = !saving, shape = RoundedCornerShape(24.dp), label = { Text("Покупка") }, leadingIcon = { Icon(Icons.Outlined.Add, null, Modifier.size(16.dp)) })
            FilterChip(selected = type == TradeType.SELL, onClick = { type = TradeType.SELL; error = null }, enabled = !saving, shape = RoundedCornerShape(24.dp), label = { Text("Продажа") }, leadingIcon = { Icon(Icons.Outlined.Remove, null, Modifier.size(16.dp)) })
        }
        Label("Счёт")
        AccountFilters(state.accounts, account, { if (!saving && it != null) { account = it; error = null } }, includeAll = false)
        ArcTextField(ticker, { ticker = it.take(40); error = null }, "Тикер", "Например, SBER", enabled = !saving,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters))
        Label("Рублёвая акция · Доступно на счёте: $available шт.")
        ArcTextField(quantity, { quantity = it.take(20); error = null }, "Количество, шт.", "Целое положительное число", enabled = !saving,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        ArcTextField(price, { price = it.take(24); error = null }, "Цена сделки, ₽", "Введите цену вручную", enabled = !saving,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        GlassCard {
            Label("Сумма операции")
            Text(total?.let { (if (type == TradeType.BUY) "−" else "+") + rubles(it) } ?: "— ₽", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = c.text)
            Label(if (type == TradeType.BUY) "Покупка · Без комиссии" else "Продажа · Средняя цена остатка сохранится")
        }
        error?.let { Text(it, color = c.negative, lineHeight = 21.sp) }
        GradientButton(if (saving) "Сохраняем…" else "Сохранить операцию", Modifier.fillMaxWidth(), enabled = !saving) {
            focus.clearFocus()
            saving = true
            save(account, ticker, type, quantity, price) { result ->
                saving = false
                error = result
                if (result == null) done()
            }
        }
        Label("Операция сохранится на этом устройстве. Дата и время будут добавлены автоматически.")
    }
}

@Composable fun ArcTextField(value: String, onValueChange: (String) -> Unit, label: String, placeholder: String = "", enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default) {
    val c = LocalArcColors.current
    OutlinedTextField(value = value, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth(), enabled = enabled,
        label = { Text(label) }, placeholder = { Text(placeholder, fontSize = 13.sp) }, singleLine = true,
        shape = RoundedCornerShape(17.dp), keyboardOptions = keyboardOptions,
        colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = c.input, focusedContainerColor = c.input,
            unfocusedBorderColor = c.muted.copy(alpha = .35f), focusedBorderColor = c.accent,
            focusedTextColor = c.text, unfocusedTextColor = c.text, unfocusedLabelColor = c.muted))
}
