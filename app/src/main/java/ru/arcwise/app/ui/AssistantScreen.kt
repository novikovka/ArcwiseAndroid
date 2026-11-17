package ru.arcwise.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.arcwise.app.AppState
import ru.arcwise.app.domain.*

@Composable fun AssistantScreen(state: AppState) {
    var draft by rememberSaveable { mutableStateOf("") }
    var messages by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    val listState = rememberLazyListState()
    val focus = LocalFocusManager.current
    val c = LocalArcColors.current
    fun send(text: String) {
        val question = text.trim()
        if (question.isEmpty()) return
        val positions = Portfolio.positions(state.trades)
        val answer = if (question.contains("добав", true) || question.contains("сделк", true) || question.contains("операц", true))
            "Откройте «Операция», выберите счёт и покупку или продажу. Укажите тикер, целое количество бумаг и цену в рублях, затем сохраните. При продаже нельзя превысить остаток на выбранном счёте."
        else if (question.contains("средн", true))
            "Средняя цена — это затраты на оставшиеся бумаги, делённые на их количество. Покупка пересчитывает среднюю, продажа сохраняет её на счёте. Для всех счетов средняя взвешивается по остаткам."
        else if (positions.isEmpty())
            "У вас ${state.accounts.size} счёта и пока нет открытых позиций. Начните с покупки: в разделе «Операция» выберите счёт, введите тикер, количество и цену сделки. Я помогу разобраться с локальным учётом."
        else "Сейчас у вас ${state.accounts.size} счёта, открытых позиций: ${positions.size}. Затраты по открытым позициям — ${rubles(positions.fold(MoneyFraction.ZERO) { sum, p -> sum + p.cost })}. ${positions.joinToString { "${it.ticker}: ${it.quantity} шт." }}. Подробности доступны в портфеле и журнале операций."
        messages = messages + listOf(question, answer)
        draft = ""
        focus.clearFocus()
    }
    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size) }
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState, contentPadding = PaddingValues(vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    BrandMark(Modifier.size(72.dp))
                    Heading("Ассистент")
                    Label("Локальная помощь по портфелю · Демо")
                    Spacer(Modifier.height(8.dp))
                    GlassCard {
                        SectionTitle("Давайте разберёмся вместе")
                        Text("Могу показать сводку и объяснить, как добавить сделку или рассчитать среднюю цену.", color = c.text, lineHeight = 22.sp)
                        Label("Ответы формируются на устройстве по шаблону. Не является инвестиционной рекомендацией.")
                    }
                }
            }
            itemsIndexed(messages) { index, message ->
                if (index % 2 == 0) Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    Text(message, Modifier.widthIn(max = 310.dp).background(c.accent.copy(alpha = .2f), RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)).padding(16.dp), color = c.text, lineHeight = 22.sp)
                } else GlassCard {
                    Text("✦ Arcwise · Демо", color = c.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(message, color = c.text, lineHeight = 22.sp)
                }
            }
        }
        if (messages.isEmpty()) Column {
            TextButton(onClick = { send("Что в моём портфеле?") }) { Text("Что в моём портфеле? →") }
            TextButton(onClick = { send("Как добавить сделку?") }) { Text("Как добавить сделку? →") }
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = draft, onValueChange = { draft = it.take(2000) }, modifier = Modifier.weight(1f),
                label = { Text("Спросите о портфеле…", fontSize = 13.sp) }, shape = RoundedCornerShape(28.dp), maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send), keyboardActions = KeyboardActions(onSend = { send(draft) }),
                colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = c.card, focusedContainerColor = c.card))
            FilledIconButton(onClick = { send(draft) }, enabled = draft.isNotBlank(), modifier = Modifier.size(48.dp)) { Icon(Icons.AutoMirrored.Outlined.ArrowForward, "Отправить сообщение") }
        }
    }
}
