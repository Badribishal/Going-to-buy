package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Thread-safe, low-allocation date formatter optimized for smooth list scrolling.
 */
object DateFormatter {
    private val threadLocalFormat = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
        }
    }

    private val reusableDate = object : ThreadLocal<Date>() {
        override fun initialValue(): Date {
            return Date()
        }
    }

    fun format(timestamp: Long): String {
        val date = reusableDate.get() ?: Date()
        date.time = timestamp
        val format = threadLocalFormat.get() ?: SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
        return format.format(date)
    }
}
