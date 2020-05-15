package org.ktugrades.common

import kotlin.js.Date

actual fun CommonDateTime.getFormatted(): String {
    val dateObj = Date(millis)
    return "${dateObj.toLocaleDateString()} ${dateObj.toLocaleTimeString()}"
}
