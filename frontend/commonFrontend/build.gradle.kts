kotlin.target.browser {
    @Suppress("EXPERIMENTAL_API_USAGE")
    dceTask {
        keep("ktor-ktor-io.\$\$importsForInline\$\$.ktor-ktor-io.io.ktor.utils.io")
    }
}