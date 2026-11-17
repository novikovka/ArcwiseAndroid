package ru.arcwise.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale
import ru.arcwise.app.domain.MoneyFraction

data class ArcColors(val dark: Boolean) {
    val text = if (dark) Color(0xFFF7F3FF) else Color(0xFF17112E)
    val muted = if (dark) Color(0xFFC7BEDD) else Color(0xFF625777)
    val accent = if (dark) Color(0xFFB398FF) else Color(0xFF6636EF)
    val border = if (dark) Color.White.copy(alpha = .22f) else Color.White.copy(alpha = .95f)
    val card = if (dark) Color.White.copy(alpha = .075f) else Color.White.copy(alpha = .72f)
    val input = if (dark) Color.White.copy(alpha = .07f) else Color.White.copy(alpha = .6f)
    val gradient = if (dark) listOf(Color(0xFF8052FF), Color(0xFF19C8DF)) else listOf(Color(0xFF7536EF), Color(0xFF265BF1))
    val positive = if (dark) Color(0xFF8AF0D2) else Color(0xFF007A5C)
    val negative = if (dark) Color(0xFFFFAAC5) else Color(0xFFB2224B)
}
val LocalArcColors = staticCompositionLocalOf { ArcColors(false) }

@Composable fun ArcTheme(dark: Boolean, content: @Composable () -> Unit) {
    val c = remember(dark) { ArcColors(dark) }
    val scheme = if (dark) darkColorScheme(primary = c.accent, background = Color(0xFF090710), surface = Color(0xFF20182F), onSurface = c.text,
        onBackground = c.text, error = c.negative, secondaryContainer = c.accent.copy(alpha = .16f), onSecondaryContainer = c.accent) else lightColorScheme(primary = c.accent, background = Color(0xFFF3ECFF), surface = Color(0xFFF9F5FF), onSurface = c.text,
        onBackground = c.text, error = c.negative, secondaryContainer = c.accent.copy(alpha = .12f), onSecondaryContainer = c.accent)
    CompositionLocalProvider(LocalArcColors provides c) { MaterialTheme(colorScheme = scheme, content = content) }
}
@Composable fun ArcBackground(content: @Composable BoxScope.() -> Unit) {
    val c = LocalArcColors.current
    Box(Modifier.fillMaxSize().background(if (c.dark) Color(0xFF090710) else Color(0xFFF5F0FF))) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.radialGradient(listOf(if (c.dark) Color(0xFF50309C) else Color(0xFFD7C7FF), Color.Transparent),
                center = Offset(0f, 0f), radius = size.width * 1.25f))
            drawRect(Brush.radialGradient(listOf(if (c.dark) Color(0xFF0C4352).copy(alpha = .8f) else Color(0xFFC3EEFB), Color.Transparent),
                center = Offset(size.width * 1.1f, size.height * .1f), radius = size.width * .95f))
            drawRect(Brush.radialGradient(listOf(if (c.dark) Color(0xFF46122F).copy(alpha = .75f) else Color(0xFFF1D4ED), Color.Transparent),
                center = Offset(size.width, size.height), radius = size.width * 1.4f))
        }
        content()
    }
}
@Composable fun GlassCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val c = LocalArcColors.current
    val shape = RoundedCornerShape(24.dp)
    Column(modifier.fillMaxWidth().background(Brush.linearGradient(listOf(c.card, c.card.copy(alpha = if (c.dark) .035f else .46f))), shape)
        .border(1.dp, c.border, shape).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}
@Composable fun GradientButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val c = LocalArcColors.current
    Button(onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 50.dp)
        .background(Brush.horizontalGradient(c.gradient.map { it.copy(alpha = if (enabled) 1f else .35f) }), RoundedCornerShape(28.dp)),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent,
            contentColor = Color.White, disabledContentColor = Color.White.copy(alpha = .6f)),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)) {
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}
@Composable fun Label(text: String, modifier: Modifier = Modifier) { Text(text, modifier, color = LocalArcColors.current.muted, fontSize = 12.sp) }
@Composable fun Heading(text: String) { Text(text, fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, color = LocalArcColors.current.text) }
@Composable fun SectionTitle(text: String) { Text(text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = LocalArcColors.current.text) }
@Composable fun BrandMark(modifier: Modifier = Modifier) {
    Canvas(modifier.size(54.dp).semantics { contentDescription = "Arcwise — кольцо и искра" }) {
        val w = size.minDimension
        val stroke = w * .15f
        val inset = stroke / 2
        val arcSize = Size(w - stroke, w - stroke)
        drawArc(Color(0xFF6D35FF), 94f, 174f, false, Offset(inset, inset), arcSize, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
        drawArc(Color(0xFF12C8EA), 278f, 62f, false, Offset(inset, inset), arcSize, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
        drawArc(Color(0xFFC9A1FA), 350f, 96f, false, Offset(inset, inset), arcSize, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
        val path = Path().apply {
            moveTo(w * .5f, w * .29f)
            quadraticBezierTo(w * .52f, w * .48f, w * .69f, w * .5f)
            quadraticBezierTo(w * .52f, w * .52f, w * .5f, w * .71f)
            quadraticBezierTo(w * .48f, w * .52f, w * .31f, w * .5f)
            quadraticBezierTo(w * .48f, w * .48f, w * .5f, w * .29f)
            close()
        }
        drawPath(path, Color(0xFF17C3EA))
    }
}
fun rubles(value: BigDecimal): String = NumberFormat.getNumberInstance(Locale("ru", "RU")).apply {
    minimumFractionDigits = 2; maximumFractionDigits = 2
}.format(value) + " ₽"
fun rubles(value: MoneyFraction) = rubles(value.rubles())
