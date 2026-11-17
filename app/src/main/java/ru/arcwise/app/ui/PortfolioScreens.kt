package ru.arcwise.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.arcwise.app.AppState
import ru.arcwise.app.data.Account
import ru.arcwise.app.domain.*
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable fun ScreenColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
}
@Composable fun OverviewScreen(state: AppState, portfolio: () -> Unit, operation: () -> Unit, assistant: () -> Unit, profile: () -> Unit) {
    val c = LocalArcColors.current
    val positions = remember(state.trades) { Portfolio.positions(state.trades) }
    val total = positions.fold(MoneyFraction.ZERO) { sum, p -> sum + p.cost }
    ScreenColumn {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BrandMark(Modifier.size(44.dp))
            Column(Modifier.weight(1f)) { Label("Всё под контролем"); Text("Мой портфель", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = c.text) }
            FilledTonalIconButton(onClick = profile, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = c.card)) { Icon(Icons.Outlined.Settings, "Открыть профиль", tint = c.text) }
        }
        Spacer(Modifier.height(4.dp))
        Label("Затраты по открытым позициям")
        Text(rubles(total), fontSize = 36.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold, color = c.text)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = portfolio, label = { Text("${state.accounts.size} счёта") }, leadingIcon = { Icon(Icons.Outlined.AccountBalanceWallet, null, Modifier.size(16.dp)) })
            AssistChip(onClick = portfolio, label = { Text("Позиций: ${positions.size}") }, leadingIcon = { Icon(Icons.Outlined.PieChartOutline, null, Modifier.size(16.dp)) })
        }
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Outlined.AutoAwesome, null, tint = c.accent)
                SectionTitle(if (positions.isEmpty()) "Начнём с первой сделки" else "Ваш портфель — в одном месте")
            }
            Text(if (positions.isEmpty()) "Два счёта уже готовы. Добавьте покупку, и Arcwise рассчитает остаток и среднюю цену." else "Позиции объединены по тикеру. Переключайте счета, чтобы посмотреть состав каждого портфеля.", color = c.text, lineHeight = 22.sp)
            GradientButton(if (positions.isEmpty()) "Добавить первую операцию" else "Открыть портфель", Modifier.fillMaxWidth(), onClick = if (positions.isEmpty()) operation else portfolio)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("Мои счета")
            TextButton(onClick = portfolio) { Text("Все →") }
        }
        state.accounts.forEach { account ->
            val items = Portfolio.positions(state.trades, account.id)
            GlassCard(onClick = portfolio) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TickerBadge("0${account.id}")
                    Column(Modifier.weight(1f)) { Text(account.name, color = c.text, fontWeight = FontWeight.SemiBold); Label("Позиций: ${items.size}") }
                    Text(rubles(items.fold(MoneyFraction.ZERO) { sum, p -> sum + p.cost }), color = c.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        GlassCard(onClick = assistant) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BrandMark(Modifier.size(36.dp))
                Column(Modifier.weight(1f)) { SectionTitle("Ассистент рядом"); Label("Локальная помощь по портфелю · Демо") }
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = c.accent)
            }
        }
        Label("Локальный учёт сделок. Цены вводятся вручную; сумма отражает затраты на оставшиеся бумаги.")
    }
}

@Composable fun AccountFilters(accounts: List<Account>, selected: Long?, onSelect: (Long?) -> Unit, includeAll: Boolean = true) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (includeAll) FilterChip(selected = selected == null, onClick = { onSelect(null) }, shape = RoundedCornerShape(24.dp), label = { Text("Все счета") })
        accounts.forEach { account -> FilterChip(selected = selected == account.id, onClick = { onSelect(account.id) }, shape = RoundedCornerShape(24.dp), label = { Text(account.name) }) }
    }
}
@Composable fun TickerBadge(ticker: String) {
    val c = LocalArcColors.current
    Box(Modifier.size(46.dp).background(c.accent.copy(alpha = .1f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
        Text(ticker.take(5), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = c.accent)
    }
}
@Composable fun PositionRow(position: Position, onClick: () -> Unit) {
    val c = LocalArcColors.current
    GlassCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TickerBadge(position.ticker)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(position.ticker, fontWeight = FontWeight.SemiBold, color = c.text)
                Label("${position.quantity} шт. · Средняя ${rubles(position.average)}")
            }
            Icon(Icons.Outlined.ChevronRight, "Открыть ${position.ticker}", tint = c.muted, modifier = Modifier.size(18.dp))
        }
        Text(rubles(position.cost), Modifier.align(Alignment.End), fontWeight = FontWeight.SemiBold, color = c.text, fontSize = 16.sp)
    }
}
@Composable fun PortfolioScreen(state: AppState, accountId: Long?, selectAccount: (Long?) -> Unit,
    openPosition: (String) -> Unit, add: () -> Unit, rename: (Long, String, (String?) -> Unit) -> Any) {
    var journal by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Account?>(null) }
    val positions = remember(state.trades, accountId) { Portfolio.positions(state.trades, accountId) }
    val c = LocalArcColors.current
    ScreenColumn {
        Label("${state.accounts.size} счёта · Локальный портфель")
        Heading("Портфель")
        GlassCard {
            Label("Затраты по открытым позициям")
            Text(rubles(positions.fold(MoneyFraction.ZERO) { sum, p -> sum + p.cost }), fontSize = 29.sp, fontWeight = FontWeight.Bold, color = c.text)
            Label("Открытых позиций: ${positions.size}")
        }
        AccountFilters(state.accounts, accountId, selectAccount)
        if (accountId != null) {
            val account = state.accounts.firstOrNull { it.id == accountId }
            TextButton(onClick = { editing = account }) { Icon(Icons.Outlined.Edit, null, Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text("Переименовать счёт") }
        }
        GradientButton("+  Добавить операцию", Modifier.fillMaxWidth(), onClick = add)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !journal, onClick = { journal = false }, shape = RoundedCornerShape(24.dp), label = { Text("Позиции") })
            FilterChip(selected = journal, onClick = { journal = true }, shape = RoundedCornerShape(24.dp), label = { Text("Журнал операций") })
        }
        if (journal) {
            val trades = state.trades.filter { accountId == null || it.accountId == accountId }.asReversed()
            if (trades.isEmpty()) EmptyCard("Журнал пока пуст", "Здесь появятся ваши покупки и продажи.")
            trades.forEach { TradeCard(it, state.accounts) }
        } else {
            if (positions.isEmpty()) EmptyCard("Пока нет открытых позиций", "Добавьте покупку на выбранный счёт. Остатки и средняя цена рассчитаются автоматически.")
            positions.forEach { p -> PositionRow(p) { openPosition(p.ticker) } }
        }
    }
    editing?.let { account -> RenameDialog(account, { editing = null }, rename) }
}
@Composable fun EmptyCard(title: String, description: String) {
    GlassCard {
        Icon(Icons.Outlined.Layers, null, tint = LocalArcColors.current.accent, modifier = Modifier.size(32.dp))
        SectionTitle(title)
        Text(description, color = LocalArcColors.current.muted, lineHeight = 22.sp)
    }
}
@Composable fun TradeCard(trade: Trade, accounts: List<Account>) {
    val c = LocalArcColors.current
    GlassCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(trade.ticker, fontWeight = FontWeight.SemiBold, color = c.text)
            Text(if (trade.type == TradeType.BUY) "Покупка" else "Продажа", color = if (trade.type == TradeType.BUY) c.positive else c.accent, fontSize = 13.sp)
        }
        Text("${trade.quantity} шт. × ${rubles(BigDecimal.valueOf(trade.priceKopecks, 2))}", color = c.text)
        Label("${accounts.firstOrNull { it.id == trade.accountId }?.name ?: "Счёт"} · ${SimpleDateFormat("dd.MM.yyyy · HH:mm", Locale("ru")).format(Date(trade.createdAt))}")
    }
}
@Composable fun PositionScreen(state: AppState, ticker: String, accountId: Long?, back: () -> Unit, buy: () -> Unit, assistant: () -> Unit) {
    val c = LocalArcColors.current
    val position = Portfolio.positions(state.trades, accountId).firstOrNull { it.ticker == ticker }
    ScreenColumn {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(onClick = back) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Назад") }
            Spacer(Modifier.width(12.dp)); Label(if (accountId == null) "Все счета · Рублёвая акция" else "${state.accounts.firstOrNull { it.id == accountId }?.name} · Рублёвая акция")
        }
        Heading(ticker)
        if (position == null) EmptyCard("Позиция закрыта", "Все операции сохранены в журнале.") else {
            Label("Затраты по открытой позиции")
            Text(rubles(position.cost), fontSize = 36.sp, fontWeight = FontWeight.Bold, color = c.text)
            GlassCard {
                Metric("Остаток", "${position.quantity} шт.")
                HorizontalDivider(color = c.border)
                Metric("Средняя цена покупки", rubles(position.average))
            }
            SectionTitle("По счетам")
            state.accounts.forEach { account ->
                Portfolio.positions(state.trades, account.id).firstOrNull { it.ticker == ticker }?.let { p ->
                    if (accountId == null || accountId == account.id) GlassCard {
                        SectionTitle(account.name)
                        Metric("Остаток", "${p.quantity} шт.")
                        Metric("Средняя цена", rubles(p.average))
                    }
                }
            }
            GlassCard(onClick = assistant) {
                SectionTitle("✦ Как считается средняя?")
                Text("Покупки учитываются по средневзвешенной цене. При продаже средняя цена оставшихся бумаг сохраняется.", color = c.muted, lineHeight = 22.sp)
                Text("Спросить ассистента →", color = c.accent, fontWeight = FontWeight.SemiBold)
            }
        }
        GradientButton("Добавить операцию с $ticker", Modifier.fillMaxWidth(), onClick = buy)
        SectionTitle("История операций")
        state.trades.filter { it.ticker == ticker && (accountId == null || it.accountId == accountId) }.asReversed().forEach { TradeCard(it, state.accounts) }
    }
}
@Composable fun Metric(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = LocalArcColors.current.muted, fontSize = 13.sp)
        Spacer(Modifier.width(10.dp))
        Text(value, color = LocalArcColors.current.text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}
@Composable fun RenameDialog(account: Account, dismiss: () -> Unit, rename: (Long, String, (String?) -> Unit) -> Any) {
    var name by rememberSaveable(account.id) { mutableStateOf(account.name) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = { if (!saving) dismiss() }, title = { Text("Название счёта") },
        text = { Column { OutlinedTextField(value = name, onValueChange = { name = it; error = null }, singleLine = true, label = { Text("Название") }, isError = error != null)
            error?.let { Text(it, color = LocalArcColors.current.negative, fontSize = 12.sp) } } },
        confirmButton = { TextButton(enabled = !saving, onClick = { saving = true; rename(account.id, name) { result -> saving = false; error = result; if (result == null) dismiss() } }) { Text("Сохранить") } },
        dismissButton = { TextButton(enabled = !saving, onClick = dismiss) { Text("Отмена") } })
}
