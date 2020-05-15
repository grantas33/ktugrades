package org.ktugrades.common

import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.*

actual fun CommonDateTime.getFormatted(): String = SimpleDateFormat("yyyy-MM-dd HH:mm").format(
    Date(millis).toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
)