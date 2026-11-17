package ru.arcwise.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.arcwise.app.AppState
import ru.arcwise.app.data.Account

@Composable fun ProfileScreen(state: AppState, setDark: (Boolean) -> Unit, logout: () -> Unit,
    rename: (Long, String, (String?) -> Unit) -> Any) {
    var editing by remember { mutableStateOf<Account?>(null) }
    val c = LocalArcColors.current
    ScreenColumn {
        Label("Ваше пространство")
        Heading("Профиль")
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(54.dp).background(c.accent.copy(alpha = .18f), RoundedCornerShape(17.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.PersonOutline, null, tint = c.accent, modifier = Modifier.size(28.dp)) }
                Column { SectionTitle("Деморежим"); Label("Локальный профиль · Без регистрации") }
            }
        }
        GlassCard {
            SectionTitle("Внешний вид")
            Label("Тема применяется сразу и сохраняется на устройстве.")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ThemeOption("Светлая", false, !state.preferences.dark, Modifier.weight(1f)) { setDark(false) }
                ThemeOption("Тёмная", true, state.preferences.dark, Modifier.weight(1f)) { setDark(true) }
            }
        }
        GlassCard {
            SectionTitle("Мои счета")
            state.accounts.forEach { account ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(account.name, Modifier.weight(1f), color = c.text)
                    IconButton(onClick = { editing = account }) { Icon(Icons.Outlined.Edit, "Переименовать ${account.name}", tint = c.accent, modifier = Modifier.size(20.dp)) }
                }
            }
        }
        GlassCard {
            SectionTitle("На вашем устройстве")
            Text("Счета и операции хранятся локально. Приложение работает без интернета и не передаёт данные на сервер.", color = c.muted, lineHeight = 22.sp)
            Label("Удаление приложения удалит локальные данные.")
        }
        GlassCard {
            SectionTitle("Сеанс")
            Text("После выхода счета и журнал останутся доступны при следующем демовходе.", color = c.muted, lineHeight = 22.sp)
            OutlinedButton(onClick = logout) { Icon(Icons.AutoMirrored.Outlined.Logout, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Выйти из деморежима") }
        }
        Label("Arcwise 0.1.0 · Учёт сделок по нескольким счетам")
    }
    editing?.let { RenameDialog(it, { editing = null }, rename) }
}
@Composable private fun ThemeOption(title: String, dark: Boolean, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = LocalArcColors.current
    Surface(onClick = onClick, modifier = modifier.border(if (selected) 2.dp else 1.dp,
        if (selected) c.accent else c.border, RoundedCornerShape(18.dp)), color = c.input, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth().height(66.dp).background(if (dark) Color(0xFF0E0A18) else Color(0xFFEDE5FF), RoundedCornerShape(11.dp)).padding(7.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.width(17.dp).fillMaxHeight().background(if (dark) Color(0xFF3D3157) else Color.White, RoundedCornerShape(5.dp)))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(Modifier.fillMaxWidth().height(13.dp).background(if (dark) Color(0xFF514765) else Color.White, RoundedCornerShape(4.dp)))
                    Box(Modifier.fillMaxWidth().weight(1f).background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color(0xFF8552F8), Color(0xFF25BEDB))), RoundedCornerShape(5.dp)))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), color = c.text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Icon(if (selected) Icons.Outlined.RadioButtonChecked else Icons.Outlined.RadioButtonUnchecked, null, tint = if (selected) c.accent else c.muted, modifier = Modifier.size(20.dp))
            }
        }
    }
}
