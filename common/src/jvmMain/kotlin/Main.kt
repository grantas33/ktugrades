package org.ktugrades.common

import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

@Serializable
actual class CommonDateTime actual constructor(val millis: Long) {
    private val dateObj: LocalDate
        get() = Date(millis).toInstant().atZone(ZoneId.systemDefault()).toLocalDate()

    actual fun getYear() = dateObj.year
}