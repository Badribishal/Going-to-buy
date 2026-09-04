package com.example.util

object CurrencyFormatter {
    private const val DEFAULT_SYMBOL = "₹"

    /**
     * Formats amounts with Indian Rupee formatting (e.g., ₹18,500, ₹1,50,000)
     */
    fun format(amount: Double, symbol: String = DEFAULT_SYMBOL): String {
        val isNegative = amount < 0
        val absAmount = Math.abs(amount)
        val longVal = absAmount.toLong()
        val fraction = ((absAmount - longVal) * 100).toLong()

        val formattedInt = formatIndianNumber(longVal)
        val result = if (fraction > 0) {
            "$formattedInt.${fraction.toString().padStart(2, '0')}"
        } else {
            formattedInt
        }

        val prefix = if (isNegative) "-$symbol" else symbol
        return "$prefix$result"
    }

    private fun formatIndianNumber(num: Long): String {
        val s = num.toString()
        if (s.length <= 3) return s
        val lastThree = s.substring(s.length - 3)
        val remaining = s.substring(0, s.length - 3)
        val groupedRemaining = remaining.reversed().chunked(2).joinToString(",").reversed()
        return "$groupedRemaining,$lastThree"
    }
}
