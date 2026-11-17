package ru.arcwise.app.domain

import org.junit.Assert.*
import org.junit.Test

class PortfolioTest {
    private fun buy(account: Long, qty: Long, price: Long, id: Long = 1) = Trade(id, account, "SBER", TradeType.BUY, qty, price, id)
    @Test fun acceptanceScenario() {
        val trades = listOf(buy(1, 2, 100000), buy(2, 3, 110000, 2))
        val all = Portfolio.positions(trades).single()
        assertEquals(5L, all.quantity)
        assertEquals("1060.00", all.average.rubles().toPlainString())
        assertEquals(2L, Portfolio.positions(trades, 1).single().quantity)
        assertEquals(3L, Portfolio.positions(trades, 2).single().quantity)
        assertThrows(IllegalArgumentException::class.java) { Portfolio.validate(1, "sber", TradeType.SELL, "3", "1200", trades) }
        val sell = Portfolio.validate(1, " sber ", TradeType.SELL, "1", "1200,00", trades)
        assertEquals("SBER", sell.ticker)
        val after = Portfolio.positions(trades + sell).single()
        assertEquals(4L, after.quantity)
        assertEquals("1075.00", after.average.rubles().toPlainString())
    }
    @Test fun exactFractionSurvivesRepeatedSalesAndNewPurchases() {
        val trades = listOf(buy(1, 2, 1, 1), buy(1, 1, 2, 2),
            Trade(3, 1, "SBER", TradeType.SELL, 1, 100, 3), buy(1, 1, 1, 4),
            Trade(5, 1, "SBER", TradeType.SELL, 1, 100, 5))
        val p = Portfolio.positions(trades).single()
        assertEquals("22", p.cost.numerator.toString())
        assertEquals("9", p.cost.denominator.toString())
    }
    @Test fun closingPositionKeepsJournalAndResetsBasis() {
        val trades = listOf(buy(1, 2, 100000), Trade(2, 1, "SBER", TradeType.SELL, 2, 120000, 2))
        assertTrue(Portfolio.positions(trades).isEmpty())
        assertEquals(2, trades.size)
        assertEquals("900.00", Portfolio.positions(trades + buy(1, 1, 90000, 3)).single().average.rubles().toPlainString())
    }
    @Test fun invalidInputRejected() {
        for ((qty, price) in listOf("0" to "100", "-1" to "100", "1.5" to "100", "1" to "0", "1" to "NaN", "1" to "1.001", "99999999999999999999" to "100")) {
            assertThrows(IllegalArgumentException::class.java) { Portfolio.validate(1, "SBER", TradeType.BUY, qty, price, emptyList()) }
        }
        assertThrows(IllegalArgumentException::class.java) { Portfolio.validate(1, " ", TradeType.BUY, "1", "100", emptyList()) }
        assertThrows(IllegalArgumentException::class.java) { Portfolio.validate(2, "SBER", TradeType.SELL, "1", "100", listOf(buy(1, 2, 100))) }
    }
}
