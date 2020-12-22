package com.github.rwsbillyang.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

fun LocalDateTime.toUtc() = toInstant(ZoneOffset.UTC).toEpochMilli()
fun Long.utcToLocalDateTime() = LocalDateTime.ofInstant(Instant.ofEpochMilli(this),ZoneOffset.UTC)

object DatetimeUtil {
    /**
     * 计算当前时间的前days天的毫秒起始值. [start,end)
     * based on java8 time package
     */
    fun getStartMilliSeconds(monthAgo: Int, daysAgo: Int): Long {
        return LocalDate.now().minusMonths(monthAgo.toLong()).minusDays(daysAgo.toLong()).atStartOfDay()
            .toInstant(ZoneOffset.UTC).toEpochMilli()
    }

    /**
     * 计算当前日期的起始毫秒数。不要包括当前秒:[start,end)
     */
    fun getTodayEndMilliSeconds(): Long {
        return LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
    }
}