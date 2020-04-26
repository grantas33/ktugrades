package org.ktugrades.common

import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

@Serializable
actual class CommonDateTime actual constructor(val millis: Long) {
    private val dateObj: LocalDate
        get() = Date(millis).toInstant().atZone(ZoneId.systemDefault()).toLocalDate()

    actual fun getFormatted() = SimpleDateFormat("yyyy-MM-dd HH:mm").format(dateObj)
}