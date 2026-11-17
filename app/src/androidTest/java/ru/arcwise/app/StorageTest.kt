package ru.arcwise.app

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import ru.arcwise.app.data.*
import ru.arcwise.app.domain.*

@RunWith(AndroidJUnit4::class)
class StorageTest {
    @Test fun journalAndAccountsSurviveDatabaseReopen() = runBlocking<Unit> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = "persistence-test.db"
        context.deleteDatabase(file)
        fun open() = Room.databaseBuilder(context, ArcwiseDatabase::class.java, file).build()
        val first = open()
        try {
            val db = first
            db.ledger().seed(listOf(Account(1, "Счёт 1"), Account(2, "Счёт 2")))
            db.ledger().rename(1, "Основной")
            db.ledger().insert(TradeRow(accountId = 1, ticker = "SBER", type = "BUY", quantity = 2, priceKopecks = 100000, createdAt = 1))
            db.ledger().insert(TradeRow(accountId = 2, ticker = "SBER", type = "BUY", quantity = 3, priceKopecks = 110000, createdAt = 2))
        } finally { first.close() }
        val second = open()
        try {
            val db = second
            db.ledger().seed(listOf(Account(1, "Счёт 1"), Account(2, "Счёт 2")))
            assertEquals("Основной", db.ledger().accounts().first().first().name)
            val position = Portfolio.positions(db.ledger().readTrades().map { it.domain() }).single()
            assertEquals(5L, position.quantity)
            assertEquals("1060.00", position.average.rubles().toPlainString())
        } finally { second.close() }
        context.deleteDatabase(file)
    }
}
