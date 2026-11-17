package ru.arcwise.app

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class AcceptanceTest {
    @get:Rule val compose = createEmptyComposeRule()
    private fun click(text: String) {
        val node = compose.onNodeWithText(text, substring = false)
        if (runCatching { node.assertIsDisplayed() }.isFailure) node.performScrollTo()
        node.performClick()
    }
    private fun awaitText(text: String, substring: Boolean = false) {
        compose.waitUntil(15000) { compose.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun screenshot(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        // Allow SurfaceFlinger to present the frame, including ripple fade-out.
        android.os.SystemClock.sleep(600)
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        instrumentation.uiAutomation.takeScreenshot().let { bitmap ->
            File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }
    private fun trade(account: String, qty: String, price: String, sell: Boolean = false) {
        click("Операция")
        click(account)
        if (sell) click("Продажа")
        compose.onNodeWithText("Тикер").performTextInput(" sber ")
        compose.onNodeWithText("Количество, шт.").performTextInput(qty)
        compose.onNodeWithText("Цена сделки, ₽").performTextInput(price)
        click("Сохранить операцию")
        compose.waitUntil(15000) {
            compose.onAllNodesWithText("Новая операция").fetchSemanticsNodes().isEmpty() ||
                compose.onAllNodesWithText("На выбранном счёте доступно", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }
    @Test fun offlineAcceptanceFlow() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitUntil(15000) { compose.onAllNodesWithText("Войти в деморежим").fetchSemanticsNodes().isNotEmpty() }
        screenshot("01-welcome-light")
        click("Войти в деморежим")
        awaitText("Мой портфель")
        compose.onNodeWithText("Мой портфель").assertExists()
        trade("Счёт 1", "2", "1000")
        compose.onNodeWithText("2 шт. · Средняя", substring = true).assertExists()
        trade("Счёт 2", "3", "1100")
        compose.onNodeWithText("5 шт. · Средняя", substring = true).assertExists()
        screenshot("02-portfolio-light")
        click("Счёт 1")
        compose.onNodeWithText("2 шт. · Средняя", substring = true).assertExists()
        click("Счёт 2")
        compose.onNodeWithText("3 шт. · Средняя", substring = true).assertExists()
        click("Все счета")
        trade("Счёт 1", "3", "1200", sell = true)
        compose.onNodeWithText("На выбранном счёте доступно 2 шт. SBER").assertExists()
        compose.onNodeWithText("Количество, шт.").performScrollTo().performTextClearance()
        compose.onNodeWithText("Количество, шт.").performTextInput("1")
        click("Сохранить операцию")
        awaitText("4 шт. · Средняя", true)
        compose.onNodeWithText("4 шт. · Средняя", substring = true).assertExists()
        click("Профиль")
        click("Тёмная")
        click("Портфель")
        screenshot("03-portfolio-dark")
        click("SBER")
        compose.onAllNodesWithText("Остаток").assertCountEquals(3)
        screenshot("04-position-dark")
        click("Ассистент")
        click("Что в моём портфеле? →")
        compose.onNodeWithText("✦ Arcwise · Демо").assertExists()
        compose.onNodeWithText("открытых позиций: 1", substring = true).assertExists()
        screenshot("05-assistant-dark")
        click("Профиль")
        compose.onNodeWithContentDescription("Переименовать Счёт 1").performScrollTo().performClick()
        compose.onNodeWithText("Название").performTextClearance()
        compose.onNodeWithText("Название").performTextInput("Основной")
        click("Сохранить")
        awaitText("Основной")
        click("Выйти из деморежима")
        awaitText("Войти в деморежим")
        compose.onNodeWithText("Войти в деморежим").assertExists()
        click("Войти в деморежим")
        awaitText("Мой портфель")
        click("Портфель")
        compose.onNodeWithText("4 шт. · Средняя", substring = true).assertExists()
        compose.onNodeWithText("Основной").assertExists()
        scenario.recreate()
        compose.onNodeWithText("4 шт. · Средняя", substring = true).assertExists()
        click("Обзор")
        screenshot("06-overview-dark")
        scenario.close()
    }
}
