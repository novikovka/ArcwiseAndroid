package ru.arcwise.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.arcwise.app.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val model: ArcwiseViewModel = viewModel()
            val state by model.state.collectAsStateWithLifecycle()
            LaunchedEffect(state.preferences.dark) {
                val style = if (state.preferences.dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            ArcTheme(state.preferences.dark) { ArcwiseApp(state, model) }
        }
    }
}
private data class Destination(val title: String, val icon: ImageVector)
private val destinations = listOf(Destination("Обзор", Icons.Outlined.GridView), Destination("Портфель", Icons.Outlined.PieChartOutline),
    Destination("Операция", Icons.Outlined.AddCircleOutline), Destination("Ассистент", Icons.Outlined.AutoAwesome), Destination("Профиль", Icons.Outlined.PersonOutline))

@Composable private fun ArcwiseApp(state: AppState, model: ArcwiseViewModel) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    var selectedAccount by rememberSaveable { mutableStateOf<Long?>(null) }
    var detail by rememberSaveable { mutableStateOf<String?>(null) }
    var tradeTicker by rememberSaveable { mutableStateOf("") }
    var tradeAccount by rememberSaveable { mutableStateOf<Long?>(null) }
    val c = LocalArcColors.current
    fun navigate(next: Int) { page = next; detail = null; if (next != 2) { tradeTicker = ""; tradeAccount = null } }
    fun newTrade(ticker: String = "", account: Long? = selectedAccount) {
        tradeTicker = ticker; tradeAccount = account; detail = null; page = 2
    }
    BackHandler(enabled = detail != null || (state.preferences.loggedIn && page != 0)) {
        if (detail != null) detail = null else navigate(0)
    }
    ArcBackground {
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            !state.preferences.loggedIn -> Welcome(state.preferences.dark, { model.dark(it) }) { model.login(true) }
            else -> Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when {
                        detail != null -> PositionScreen(state, detail!!, selectedAccount, { detail = null }, { newTrade(detail!!) }, { navigate(3) })
                        page == 0 -> OverviewScreen(state, { navigate(1) }, { newTrade() }, { navigate(3) }, { navigate(4) })
                        page == 1 -> PortfolioScreen(state, selectedAccount, { selectedAccount = it }, { detail = it }, { newTrade() }, model::rename)
                        page == 2 -> OperationScreen(state, tradeTicker, tradeAccount, model::save) { navigate(1) }
                        page == 3 -> AssistantScreen(state)
                        else -> ProfileScreen(state, { model.dark(it) }, { model.login(false); navigate(0) }, model::rename)
                    }
                }
                Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp).fillMaxWidth()
                    .background(if (c.dark) Color(0xFF22182F).copy(alpha = .95f) else Color(0xFFFAF7FF).copy(alpha = .95f), RoundedCornerShape(30.dp))
                    .border(1.dp, c.accent.copy(alpha = .32f), RoundedCornerShape(30.dp)).padding(horizontal = 3.dp, vertical = 5.dp)) {
                    destinations.forEachIndexed { index, destination ->
                        val selected = page == index
                        Surface(onClick = { if (index == 2 && page != 2) newTrade() else navigate(index) },
                            modifier = Modifier.weight(1f), shape = RoundedCornerShape(23.dp),
                            color = if (selected) c.accent.copy(alpha = .12f) else Color.Transparent) {
                            Column(Modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Icon(destination.icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = if (selected) c.accent else c.text)
                                Text(destination.title, fontSize = 9.sp, maxLines = 1, color = if (selected) c.accent else c.text)
                            }
                        }
                    }
                }
            }
        }
        state.error?.let { error -> AlertDialog(onDismissRequest = model::clearError,
            title = { Text("Не удалось выполнить действие") }, text = { Text(error) },
            confirmButton = { TextButton(onClick = model::clearError) { Text("Понятно") } }) }
    }
}

@Composable private fun Welcome(dark: Boolean, onDark: (Boolean) -> Unit, login: () -> Unit) {
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.End) {
        OutlinedButton(onClick = { onDark(!dark) }) { Icon(if (dark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode, null, Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text(if (dark) "Светлая тема" else "Тёмная тема") }
        Spacer(Modifier.height(100.dp))
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BrandMark(); Text("Arcwise", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = LocalArcColors.current.text)
            }
            Spacer(Modifier.height(6.dp))
            Heading("Ваш портфель.\nВсё на своих местах.")
            Text("Учитывайте сделки и объединяйте счета в одну понятную картину.", color = LocalArcColors.current.muted, lineHeight = 22.sp)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.WifiOff, null, tint = LocalArcColors.current.accent, modifier = Modifier.size(20.dp))
                Label("Работает офлайн · Данные на устройстве")
            }
            GradientButton("Войти в деморежим", Modifier.fillMaxWidth(), onClick = login)
            Label("Без регистрации. Начните с двух пустых счетов и добавьте свою первую сделку.")
        }
        Spacer(Modifier.height(24.dp))
        Text("ARCWISE  /  0.1", Modifier.fillMaxWidth(), color = LocalArcColors.current.muted, fontSize = 11.sp, letterSpacing = 3.sp)
    }
}
