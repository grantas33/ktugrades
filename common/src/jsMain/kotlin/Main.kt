package org.ktugrades.common

import kotlinx.serialization.*
import kotlin.js.Date

@Serializable
actual class CommonDateTime actual constructor(val millis: Long) {
    private val dateObj: Date
        get() = Date(millis)

    actual fun getFormatted() = "${dateObj.toLocaleDateString()} ${dateObj.toLocaleTimeString()}"
}
