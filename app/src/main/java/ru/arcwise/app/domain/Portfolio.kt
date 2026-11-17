package ru.arcwise.app.domain

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.util.Locale

enum class TradeType { BUY, SELL }
data class Trade(val id: Long = 0, val accountId: Long, val ticker: String, val type: TradeType,
                 val quantity: Long, val priceKopecks: Long, val createdAt: Long = System.currentTimeMillis())

/** Exact rational kopecks: partial sales never round the remaining cost basis. */
data class MoneyFraction private constructor(val numerator: BigInteger, val denominator: BigInteger) {
    operator fun plus(other: MoneyFraction) = of(numerator * other.denominator + other.numerator * denominator, denominator * other.denominator)
    fun times(value: Long) = of(numerator * value.toBigInteger(), denominator)
    fun dividedBy(value: Long) = of(numerator, denominator * value.toBigInteger())
    fun rubles(): BigDecimal = numerator.toBigDecimal().divide(denominator.toBigDecimal() * BigDecimal(100), 2, RoundingMode.HALF_UP)
    companion object {
        val ZERO = of(BigInteger.ZERO, BigInteger.ONE)
        fun kopecks(value: Long) = of(value.toBigInteger(), BigInteger.ONE)
        private fun of(n: BigInteger, d: BigInteger): MoneyFraction {
            require(d.signum() > 0)
            val gcd = n.gcd(d)
            return MoneyFraction(n / gcd, d / gcd)
        }
    }
}
data class Position(val accountId: Long?, val ticker: String, val quantity: Long, val cost: MoneyFraction) {
    val average: MoneyFraction get() = cost.dividedBy(quantity)
}
object Portfolio {
    fun positions(trades: List<Trade>, accountId: Long? = null): List<Position> {
        val holdings = linkedMapOf<Pair<Long, String>, Position>()
        trades.sortedWith(compareBy<Trade> { it.createdAt }.thenBy { it.id }).forEach { trade ->
            if (accountId != null && trade.accountId != accountId) return@forEach
            val key = trade.accountId to trade.ticker
            val old = holdings[key] ?: Position(trade.accountId, trade.ticker, 0, MoneyFraction.ZERO)
            val next = when (trade.type) {
                TradeType.BUY -> Position(trade.accountId, trade.ticker, Math.addExact(old.quantity, trade.quantity),
                    old.cost + MoneyFraction.kopecks(trade.priceKopecks).times(trade.quantity))
                TradeType.SELL -> {
                    require(trade.quantity <= old.quantity) { "Недостаточно бумаг на выбранном счёте" }
                    val remaining = old.quantity - trade.quantity
                    Position(trade.accountId, trade.ticker, remaining,
                        if (remaining == 0L) MoneyFraction.ZERO else old.cost.times(remaining).dividedBy(old.quantity))
                }
            }
            holdings[key] = next
        }
        return holdings.values.filter { it.quantity > 0 }.groupBy { it.ticker }.map { (ticker, values) ->
            Position(accountId, ticker, values.fold(0L) { sum, p -> Math.addExact(sum, p.quantity) },
                values.fold(MoneyFraction.ZERO) { sum, p -> sum + p.cost })
        }.sortedBy { it.ticker }
    }

    fun validate(accountId: Long, tickerInput: String, type: TradeType, quantityInput: String,
                 priceInput: String, trades: List<Trade>): Trade {
        val ticker = tickerInput.trim().uppercase(Locale.ROOT)
        require(Regex("[A-ZА-ЯЁ0-9][A-ZА-ЯЁ0-9._-]{0,15}").matches(ticker)) { "Введите тикер: до 16 букв, цифр, точек или дефисов" }
        val quantity = quantityInput.trim().toLongOrNull()
        require(quantity != null && quantity in 1..1_000_000_000L) { "Количество — целое число от 1 до 1 000 000 000" }
        val rawPrice = priceInput.trim().replace(',', '.')
        require(Regex("[0-9]+(\\.[0-9]{1,2})?").matches(rawPrice)) { "Цена — положительное число, не более двух знаков после запятой" }
        val price = rawPrice.toBigDecimalOrNull()
        require(price != null && price > BigDecimal.ZERO && price <= BigDecimal("1000000000000")) { "Цена должна быть от 0,01 до 1 000 000 000 000 ₽" }
        if (type == TradeType.SELL) {
            val available = positions(trades, accountId).firstOrNull { it.ticker == ticker }?.quantity ?: 0L
            require(quantity <= available) { "На выбранном счёте доступно $available шт. $ticker" }
        }
        return Trade(accountId = accountId, ticker = ticker, type = type, quantity = quantity,
            priceKopecks = price.movePointRight(2).longValueExact())
    }
}
