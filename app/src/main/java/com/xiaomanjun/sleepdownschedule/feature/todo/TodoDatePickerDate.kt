package com.xiaomanjun.sleepdownschedule.feature.todo

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private val TodoDatePickerZone = ZoneId.of("UTC")

internal fun todoDatePickerMillis(date: LocalDate?): Long? =
    date?.atStartOfDay(TodoDatePickerZone)?.toInstant()?.toEpochMilli()

internal fun todoDatePickerDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(TodoDatePickerZone).toLocalDate()
