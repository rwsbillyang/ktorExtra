package com.github.rwsbillyang.util


import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

fun LocalDateTime.toUtc() = toInstant(ZoneOffset.UTC).toEpochMilli()
fun Long.utcToLocalDateTime(): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(this),ZoneOffset.UTC)
fun Long.plusTime(years: Long = 0L, months: Long = 0L, days: Long = 0L) =
    utcToLocalDateTime().plusYears(years).plusMonths(months).plusDays(days).toUtc()

object DatetimeUtil {
    private val log = LoggerFactory.getLogger("DatetimeUtil")
    /**
     * 计算当前时间的前days天的毫秒起始值. [start,end)
     * based on java8 time package
     */
    fun getStartMilliSeconds(daysAgo: Int, monthAgo: Int = 0): Long {
        return LocalDate.now().minusMonths(monthAgo.toLong()).minusDays(daysAgo.toLong()).atStartOfDay()
            .toInstant(ZoneOffset.UTC).toEpochMilli()
    }

    /**
     * 计算当前日期的起始毫秒数。不要包括当前秒:[start,end)
     */
    fun getTodayStartMilliSeconds(): Long {
        return LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
    }

    /**
     * yyyy-MM-dd hh:mm:ss
     * */
    fun parse(dateTime: String, format: String = "yyyy-MM-dd hh:mm:ss"): LocalDateTime?{
        return try {
            LocalDateTime.parse(dateTime, DateTimeFormatter.ofPattern(format))
        }catch (e: DateTimeParseException){
            log.warn("DateTimeParseException: $dateTime")
            null
        }
    }
}