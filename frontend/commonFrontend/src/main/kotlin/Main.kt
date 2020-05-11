import org.ktugrades.common.MarkInfoResponse

const val CACHE_USERNAME = "username"
const val DATA_CACHE = "dataCache"

const val SERVER_URL = ""
const val CLIENT_URL = ""

inline fun jsObject(init: dynamic.() -> Unit): dynamic {
    val o = js("{}")
    init(o)
    return o
}

val applicationJsonHeaders = jsObject { this["Content-type"] = "application/json" }

fun List<MarkInfoResponse>.sort() = this
    .sortedWith(compareByDescending<MarkInfoResponse> {it.date.millis}
        .thenByDescending {it.semesterCode }
        .thenByDescending {it.week.split("-").map {it.toInt()}.average()}
        .thenBy {it.title}
    )

